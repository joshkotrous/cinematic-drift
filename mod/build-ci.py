"""Build without an installed Minecraft profile (Python 3.11+, JDK 25)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parent
BUILD = ROOT / "build" / "ci"
DEPS = BUILD / "deps"
HEADERS = {"User-Agent": "CinematicDrift-build/1.0 (github.com/joshkotrous/cinematic-drift)"}


def request(url):
    return urllib.request.urlopen(urllib.request.Request(url, headers=HEADERS), timeout=120)


def metadata(url):
    with request(url) as response:
        return json.load(response)


def download(spec):
    url, name, expected = spec
    target = DEPS / name
    target.parent.mkdir(parents=True, exist_ok=True)
    if not target.exists() or (expected and hashlib.sha1(target.read_bytes()).hexdigest() != expected):
        with request(url) as response:
            data = response.read()
        if expected and hashlib.sha1(data).hexdigest() != expected:
            raise RuntimeError(f"Checksum mismatch: {name}")
        target.write_bytes(data)
    return target


def maven(name, base="https://maven.fabricmc.net/"):
    group, artifact, version = name.split(":")
    path = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.jar"
    return base + path, path, None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--java-home", default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--build-id", default="")
    args = parser.parse_args()
    mod = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text())
    minecraft = mod["depends"]["minecraft"]
    manifest = metadata("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
    version = next(v for v in manifest["versions"] if v["id"] == minecraft)
    game = metadata(version["url"])
    client = game["downloads"]["client"]
    downloads = [(client["url"], "minecraft.jar", client["sha1"])]
    for library in game["libraries"]:
        artifact = library.get("downloads", {}).get("artifact")
        if artifact:
            downloads.append((artifact["url"], artifact["path"], artifact["sha1"]))
    loader_version = mod["depends"]["fabricloader"].removeprefix(">=")
    loader = metadata(f"https://meta.fabricmc.net/v2/versions/loader/{minecraft}/{loader_version}")
    downloads.append(maven(f"net.fabricmc:fabric-loader:{loader_version}"))
    for library in loader["launcherMeta"]["libraries"]["common"]:
        url, path, _ = maven(library["name"], library["url"])
        downloads.append((url, path, library.get("sha1")))
    api_version = mod["depends"]["fabric-api"].removeprefix(">=") + "+" + minecraft
    api_spec = maven(f"net.fabricmc.fabric-api:fabric-api:{api_version}")
    iris_versions = metadata(f"https://api.modrinth.com/v2/project/iris/version?game_versions=%5B%22{minecraft}%22%5D")
    iris = next(v for v in iris_versions if v["version_number"] == mod["depends"]["iris"] + "+" + minecraft + "-fabric")
    iris_file = next(f for f in iris["files"] if f["primary"])
    downloads += [api_spec, (iris_file["url"], "iris.jar", iris_file["hashes"]["sha1"])]
    with ThreadPoolExecutor(max_workers=8) as pool:
        jars = list(pool.map(download, downloads))
    api = DEPS / api_spec[1]
    with zipfile.ZipFile(api) as archive:
        for name in archive.namelist():
            if name.startswith("META-INF/jars/fabric-") and name.endswith(".jar") and ("command-api" in name or "api-base" in name):
                target = DEPS / Path(name).name
                target.write_bytes(archive.read(name))
                jars.append(target)
    classes = BUILD / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    suffix = ".exe" if os.name == "nt" else ""
    def java_tool(name):
        return str(Path(args.java_home) / "bin" / (name + suffix)) if args.java_home else name
    subprocess.run([java_tool("javac"), "--release", "25", "-proc:none", "-classpath", os.pathsep.join(map(str, jars)), "-d", str(classes), *map(str, (ROOT / "src/main/java").rglob("*.java"))], check=True)
    test_classes = BUILD / "test-classes"
    test_classes.mkdir(exist_ok=True)
    subprocess.run([java_tool("javac"), "--release", "25", "-classpath", str(classes), "-d", str(test_classes), str(ROOT / "src/test/java/FlowFieldStreamingTest.java")], check=True)
    subprocess.run([java_tool("java"), "-classpath", os.pathsep.join(map(str, [classes, test_classes])), "FlowFieldStreamingTest"], check=True)
    shutil.copytree(ROOT / "src/main/resources", classes, dirs_exist_ok=True)
    if args.build_id:
        mod["version"] += "+build." + args.build_id
    (classes / "fabric.mod.json").write_text(json.dumps(mod, indent=2) + "\n")
    shutil.copyfile(ROOT / "LICENSE", classes / "LICENSE")
    output = BUILD / f"ridge-clouds-{mod['version']}+mc{minecraft}.jar"
    subprocess.run([java_tool("jar"), "--create", "--file", str(output), "-C", str(classes), "."], check=True)
    with zipfile.ZipFile(output) as archive:
        if "dev/ridgeclouds/client/RidgeCloudsClient.class" not in archive.namelist():
            raise RuntimeError("Missing mod entrypoint")
        assert json.loads(archive.read("fabric.mod.json"))["version"] == mod["version"]
    (BUILD / (output.name + ".sha256")).write_text(hashlib.sha256(output.read_bytes()).hexdigest() + "  " + output.name + "\n")
    print(f"Built and verified {output.name}")


if __name__ == "__main__":
    main()
