"""Smoke-test packaged artifacts in disposable, loopback-only servers.

Requires explicit Minecraft EULA acceptance. Logs and results are retained under
the supplied report directory; each installed server is removed after it stops.
Uses only Python's standard library and the repository's existing Java runtimes.
"""

import argparse
import concurrent.futures
import hashlib
import json
import os
import re
from pathlib import Path
import queue
import shutil
import sys
import subprocess
import threading
import time
import urllib.request
import urllib.parse
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True


REPO = Path(__file__).resolve().parents[1]
USER_AGENT = "wUtilities-verification/0.1 (https://github.com/wSmoothie/wUtilities)"
RELEASE_VERSION = re.search(r'mod.version\s*=\s*"([^"]+)"',
                            (REPO / "stonecutter.properties.toml").read_text()).group(1)
REPORT_ROOT = Path("V:/Documents/ChatGPT/local-reports/wUtilities").resolve()
JAVA = {}
JAVA_LOCK = threading.Lock()


def discover_java(major):
    """Prefer explicit Java homes, then PATH and installed Gradle toolchains."""
    with JAVA_LOCK:
        if major in JAVA:
            return JAVA[major]
        executable = "java.exe" if os.name == "nt" else "java"
        candidates = []
        for key in (f"WUTILITIES_JAVA_{major}", f"JAVA{major}_HOME", f"JAVA_HOME_{major}", "JAVA_HOME"):
            value = os.environ.get(key)
            if value:
                path = Path(value)
                candidates.append(path / "bin" / executable if path.is_dir() else path)
        on_path = shutil.which("java")
        if on_path:
            candidates.append(Path(on_path))
        roots = [Path.home() / ".gradle/jdks", Path.home() / ".jdks"]
        if os.name == "nt":
            program_files = Path(os.environ.get("ProgramFiles", "C:/Program Files"))
            roots.extend(program_files / name for name in ("Java", "Eclipse Adoptium", "Microsoft", "Zulu"))
        else:
            roots.extend([Path("/usr/lib/jvm"), Path("/Library/Java/JavaVirtualMachines")])
        for root in roots:
            candidates.extend(root.glob(f"*/bin/{executable}"))
            candidates.extend(root.glob(f"*/Contents/Home/bin/{executable}"))
        for candidate in dict.fromkeys(candidates):
            if not candidate.is_file():
                continue
            version = subprocess.run([str(candidate), "-version"], capture_output=True,
                                     text=True, errors="replace", timeout=15)
            match = re.search(r'version "(\d+)', version.stdout + version.stderr)
            if version.returncode == 0 and match and int(match.group(1)) == major:
                JAVA[major] = candidate.resolve()
                return JAVA[major]
        raise RuntimeError(f"Java {major} is required; set JAVA{major}_HOME or WUTILITIES_JAVA_{major}")


def api(url):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=90) as response:
        return json.load(response)


def download(url, destination, digest=None, algorithm="sha256"):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=180) as response:
        with destination.open("wb") as output:
            shutil.copyfileobj(response, output)
    if digest and hashlib.new(algorithm, destination.read_bytes()).hexdigest() != digest:
        raise RuntimeError(f"Checksum mismatch for {destination.name}")


def native_version(loader, minecraft):
    text = (REPO / "stonecutter.properties.toml").read_text()
    match = re.search(
        rf'\[{loader}\."{re.escape(minecraft)}"\]\s*.*?deps\.{loader}\s*=\s*"([^"]+)"',
        text, re.DOTALL,
    )
    if match:
        return match.group(1)
    host = "https://maven.neoforged.net/releases/net/neoforged/neoforge" if loader == "neoforge" else "https://maven.minecraftforge.net/net/minecraftforge/forge"
    with urllib.request.urlopen(f"{host}/maven-metadata.xml", timeout=90) as response:
        versions = [item.text for item in ET.fromstring(response.read()).findall(".//version")]
    if loader == "neoforge":
        parts = minecraft.split(".")
        prefix = f"{parts[1]}.{parts[2] if len(parts) > 2 else '0'}."
    else:
        prefix = f"{minecraft}-"
    candidates = [version for version in versions if version.startswith(prefix)]
    if not candidates:
        raise RuntimeError(f"No native {loader} release for {minecraft}")
    return max(candidates, key=lambda version: tuple(map(int, re.findall(r'\d+', version))))


def java_for(minecraft):
    if minecraft.startswith("26."):
        return discover_java(25)
    if tuple(map(int, minecraft.split("."))) >= (1, 20, 5):
        return discover_java(21)
    return discover_java(17)


def install_fabric(minecraft, directory):
    loaders = api(f"https://meta.fabricmc.net/v2/versions/loader/{minecraft}")
    loader = next(item["loader"]["version"] for item in loaders if item["loader"]["stable"])
    installer = next(item["version"] for item in api(
        "https://meta.fabricmc.net/v2/versions/installer") if item["stable"])
    download(f"https://meta.fabricmc.net/v2/versions/loader/{minecraft}/{loader}/{installer}/server/jar",
             directory / "server.jar")
    # Select Fabric API from the configured build dependency, not an unrelated latest release.
    text = (REPO / "stonecutter.properties.toml").read_text()
    match = re.search(rf'\[fabric\."{re.escape(minecraft)}"\]\s*.*?deps\.fabric_api\s*=\s*"([^"]+)"', text, re.DOTALL)
    if match:
        version = match.group(1)
        download(f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{version}/fabric-api-{version}.jar",
                 directory / "mods/fabric-api.jar")
    else:
        versions = api("https://api.modrinth.com/v2/project/fabric-api/version?" + urllib.parse.urlencode(
            {"loaders": '["fabric"]', "game_versions": json.dumps([minecraft])}))
        artifact = next(file for file in versions[0]["files"] if file["primary"])
        download(artifact["url"], directory / "mods/fabric-api.jar", artifact["hashes"]["sha512"], "sha512")
    return ["-jar", "server.jar", "nogui"]


def install_quilt(minecraft, directory, java, log):
    # Install Fabric API using the same compatible version, then replace only the launcher.
    install_fabric(minecraft, directory)
    installer = api("https://meta.quiltmc.org/v3/versions/installer")[0]
    # Quilt's metadata API hashes can lag a republished installer; verify against
    # the checksum published beside the exact Maven artifact instead.
    with urllib.request.urlopen(installer["url"] + ".sha256", timeout=90) as response:
        checksum = response.read().decode().strip().split()[0]
    download(installer["url"], directory / "quilt-installer.jar", checksum)
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run([str(java), "-jar", "quilt-installer.jar", "install", "server",
                                 minecraft, "0.30.0", "--download-server", "--install-dir=."], cwd=directory,
                                stdout=output, stderr=subprocess.STDOUT, timeout=300)
    if result.returncode:
        raise RuntimeError(f"Quilt installer exited {result.returncode}; see {log.name}")
    launchers = list(directory.glob("quilt-server-launch*.jar"))
    if len(launchers) != 1:
        raise RuntimeError(f"Expected Quilt launcher; found {len(launchers)}")
    return ["-jar", launchers[0].name, "nogui"]


def install_native(loader, minecraft, directory, java, log):
    version = native_version(loader, minecraft)
    module = "net.neoforged/neoforge" if loader == "neoforge" else "net.minecraftforge/forge"
    artifact_version = version if loader == "neoforge" else f"{minecraft}-{version}"
    cached = Path.home() / ".gradle/caches/modules-2/files-2.1" / module / artifact_version
    candidates = list(cached.glob("*/*-installer.jar"))
    installer = directory / "installer.jar"
    if candidates:
        shutil.copy2(candidates[0], installer)
    else:
        host = "https://maven.neoforged.net/releases" if loader == "neoforge" else "https://maven.minecraftforge.net"
        download(f"{host}/{module.replace('.', '/')}/{artifact_version}/{loader}-{artifact_version}-installer.jar",
                 installer)
    with log.open("w", encoding="utf-8") as output:
        result = subprocess.run([str(java), "-jar", str(installer), "--installServer"], cwd=directory,
                                stdout=output, stderr=subprocess.STDOUT, timeout=1200)
    if result.returncode:
        raise RuntimeError(f"Installer exited {result.returncode}; see {log.name}")
    args = list(directory.glob("libraries/**/win_args.txt"))
    if len(args) != 1:
        raise RuntimeError(f"Expected one loader argument file; found {len(args)}")
    return [f"@{args[0].relative_to(directory)}", "nogui"]


def install_bukkit(minecraft, directory, kind="paper"):
    if kind in ("paper", "folia"):
        builds = api(f"https://fill.papermc.io/v3/projects/{kind}/versions/{minecraft}/builds")
        selected = max(builds, key=lambda item: item["id"])
        artifact = selected["downloads"]["server:default"]
        download(artifact["url"], directory / "server.jar", artifact["checksums"]["sha256"])
    elif kind == "purpur":
        metadata = api(f"https://api.purpurmc.org/v2/purpur/{minecraft}")
        download(f"https://api.purpurmc.org/v2/purpur/{minecraft}/{metadata['builds']['latest']}/download",
                 directory / "server.jar")
    else:
        metadata = api(f"https://api.leafmc.one/v2/projects/leaf/versions/{minecraft}/builds")
        selected = max(metadata["builds"], key=lambda item: item["build"])
        artifact = selected["downloads"]["primary"]
        download(f"https://api.leafmc.one/v2/projects/leaf/versions/{minecraft}/builds/{selected['build']}/downloads/{artifact['name']}",
                 directory / "server.jar", artifact.get("sha256"))
    return ["-jar", "server.jar", "nogui"]


def launch(java, args, directory, log, timeout=300):
    lines = queue.Queue()
    process = subprocess.Popen([str(java), "-Xms128M", "-Xmx1G", *args], cwd=directory,
                               stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
    def consume():
        for line in process.stdout:
            lines.put(line)
        lines.put(None)
    reader = threading.Thread(target=consume, daemon=True)
    reader.start()
    enabled = False
    ready = False
    deadline = time.monotonic() + timeout
    try:
        with log.open("w", encoding="utf-8") as output:
            while time.monotonic() < deadline:
                try:
                    line = lines.get(timeout=1)
                except queue.Empty:
                    if process.poll() is not None:
                        break
                    continue
                if line is None:
                    break
                output.write(line)
                output.flush()
                if "disabledWaypointsFeatures=" in line:
                    enabled = True
                if "Done (" in line and 'For help, type "help"' in line:
                    ready = True
                    process.stdin.write("stop\n")
                    process.stdin.flush()
            if process.poll() is None:
                try:
                    process.stdin.write("stop\n")
                    process.stdin.flush()
                    process.wait(timeout=60)
                except OSError:
                    process.wait(timeout=60)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait(timeout=30)
            reader.join(timeout=5)
            while not lines.empty():
                line = lines.get_nowait()
                if line:
                    output.write(line)
    finally:
        if process.poll() is None:
            process.kill()
            process.wait(timeout=30)
    if not ready or not enabled or process.returncode:
        raise RuntimeError(f"Startup failed: ready={ready}, utilityLoaded={enabled}, exit={process.returncode}; see {log.name}")
    return log.read_text(encoding="utf-8")


def verify(target, report, index):
    loader, minecraft, label = target
    name = f"{minecraft}-{loader}"
    workspace = (report / "disposable-servers" / name).resolve()
    allowed = (report / "disposable-servers").resolve()
    if workspace.parent != allowed or workspace.exists():
        raise RuntimeError(f"Refusing existing or unexpected server directory: {workspace}")
    result = {"target": name, "cohort": label, "passed": False, "cleaned": False}
    logs = report / name
    logs.mkdir(parents=True, exist_ok=True)
    started = time.monotonic()
    try:
        workspace.mkdir(parents=True)
        (workspace / "eula.txt").write_text("# User accepted EULA for local verification\neula=true\n")
        (workspace / "server.properties").write_text(
            f"server-ip=127.0.0.1\nserver-port={26000 + index}\nonline-mode=false\n"
            "enable-query=false\nenable-rcon=false\nview-distance=2\nsimulation-distance=2\n"
            "max-players=2\nmax-tick-time=-1\nlevel-type=minecraft:flat\n"
            'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1}],"biome":"minecraft:plains"}\n'
        )
        is_bukkit = loader in ("bukkit", "paper", "folia", "purpur", "leaf")
        install = "plugins" if is_bukkit else "mods"
        (workspace / install).mkdir()
        family = "Bukkit" if is_bukkit else {"fabric": "Fabric", "quilt": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}[loader]
        artifact = REPO / "build/artifacts" / f"wUtilities {RELEASE_VERSION} {family} {label}.jar"
        if not artifact.is_file():
            raise RuntimeError(f"Packaged artifact missing: {artifact.name}")
        shutil.copy2(artifact, workspace / install / "wUtilities.jar")
        result["artifact"] = artifact.name
        result["sha256"] = hashlib.sha256(artifact.read_bytes()).hexdigest()
        java = java_for(minecraft)
        if loader == "fabric":
            args = install_fabric(minecraft, workspace)
        elif loader == "quilt":
            args = install_quilt(minecraft, workspace, java, logs / "install.log")
        elif is_bukkit:
            args = install_bukkit(minecraft, workspace, "paper" if loader == "bukkit" else loader)
        else:
            args = install_native(loader, minecraft, workspace, java, logs / "install.log")
        default_log = launch(java, args, workspace, logs / "unrestricted.log")
        config_relative = Path("plugins/wUtilities" if is_bukkit else "config") / "wutilities.properties"
        config = workspace / config_relative
        if not config.is_file() or "disabledWaypointsFeatures=[]" not in default_log:
            raise RuntimeError("Default configuration was not generated unrestricted")
        shutil.copy2(config, logs / "generated-wutilities.properties")
        config.write_text("disable-wworldmap=false\ndisabled-wworldmap-features=entity-radar,cave-mode\n"
                          "disable-wwaypoints=false\ndisabled-wwaypoints-features=sneak-modifications,sign-modifications\n")
        restricted_log = launch(java, args, workspace, logs / "restricted.log")
        if "disabledWaypointsFeatures=[sneak-modifications, sign-modifications]" not in restricted_log:
            raise RuntimeError("Configured restrictions did not load after restart")
        result.update(passed=True, config=str(config_relative).replace("\\", "/"),
                      checks=["packaged artifact startup", "unrestricted config generation", "restriction config restart"])
    except Exception as exception:
        result["error"] = str(exception)
    finally:
        # Only delete this invocation's exact disposable directory after launch() stopped its process.
        if workspace.exists():
            if workspace.parent != allowed:
                raise RuntimeError(f"Unsafe cleanup target: {workspace}")
            shutil.rmtree(workspace)
        result["cleaned"] = not workspace.exists()
        result["elapsedSeconds"] = round(time.monotonic() - started, 1)
        (logs / "result.json").write_text(json.dumps(result, indent=2))
    print(json.dumps(result), flush=True)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--accept-eula", action="store_true", help="Only use after explicit user acceptance")
    parser.add_argument("--target", action="append", help="Filter target IDs, for example 1.21.11-fabric")
    parser.add_argument("--loader", action="append", help="Filter loader IDs, for example quilt or neoforge")
    parser.add_argument("--workers", type=int, choices=(1, 2), default=1)
    parser.add_argument("--resume", action="store_true", help="Reuse passing results only for an identical artifact hash")
    args = parser.parse_args()
    if not args.accept_eula:
        parser.error("Explicit Minecraft EULA acceptance is required before server verification")
    report = args.report.resolve()
    if report != REPORT_ROOT and REPORT_ROOT not in report.parents:
        parser.error(f"Generated evidence must be under {REPORT_ROOT}")
    if report == REPO or REPO in report.parents or any((parent / ".git").exists() for parent in [report, *report.parents]):
        parser.error("Generated evidence and disposable servers must be outside the repository")
    support = json.loads((REPO / "supported-versions.json").read_text())
    def members(label):
        if "-" not in label:
            return [label]
        first, last = label.split("-")
        if "." in last:
            return [first, last]
        base, patch = first.rsplit(".", 1)
        return [f"{base}.{value}" for value in range(int(patch), int(last) + 1)]
    targets = [("paper", minecraft, "1.20+") for minecraft in ("1.20.1", "1.21.11", "26.2")]
    targets += [(kind, "1.21.11", "1.20+") for kind in ("folia", "purpur", "leaf")]
    targets += [(loader, minecraft, label) for loader in ("fabric", "quilt")
                for label in support["fabricQuiltCohorts"] for minecraft in members(label)]
    targets += [("neoforge", minecraft, label) for label in support["neoForgeCohorts"] for minecraft in members(label)]
    targets += [("forge", version, version) for version in support["forge"]]
    if args.loader:
        targets = [target for target in targets if target[0] in args.loader]
        if not targets:
            parser.error("Unknown loader filter")
    if args.target:
        targets = [target for target in targets if f"{target[1]}-{target[0]}" in args.target]
        if len(targets) != len(set(args.target)):
            parser.error("Unknown or duplicate target filter")
    report.mkdir(parents=True, exist_ok=True)
    results = []
    pending = []
    for index, target in enumerate(targets):
        previous_path = report / f"{target[1]}-{target[0]}" / "result.json"
        if args.resume and previous_path.is_file():
            previous = json.loads(previous_path.read_text())
            artifact = REPO / "build/artifacts" / previous.get("artifact", "missing")
            if previous.get("passed") and previous.get("cleaned") and artifact.is_file():
                if previous.get("sha256") == hashlib.sha256(artifact.read_bytes()).hexdigest():
                    results.append(previous)
                    continue
        pending.append((index, target))
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool:
        futures = [pool.submit(verify, target, report, index) for index, target in pending]
        results.extend(future.result() for future in futures)
    (report / "server-matrix.json").write_text(json.dumps(results, indent=2))
    print(f"Passed {sum(item['passed'] for item in results)}/{len(results)}; all cleaned={all(item['cleaned'] for item in results)}")
    raise SystemExit(0 if all(item["passed"] for item in results) else 1)


if __name__ == "__main__":
    main()
