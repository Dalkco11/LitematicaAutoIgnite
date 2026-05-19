import os
import shutil
import subprocess
import re

targets = [
    {
        "mc": "26.1",
        "fabric": "0.144.1+26.1"
    },
    {
        "mc": "26.1.1",
        "fabric": "0.145.2+26.1.1"
    },
    {
        "mc": "26.1.2",
        "fabric": "0.149.0+26.1.2"
    }
]

os.makedirs("release", exist_ok=True)

with open("gradle.properties", "r", encoding="utf-8") as f:
    gp_content = f.read()

with open("src/main/resources/fabric.mod.json", "r", encoding="utf-8") as f:
    fmj_content = f.read()

gp_content = re.sub(r"mod_version=.*", "mod_version=1.0.0", gp_content)

for target in targets:
    mc_ver = target["mc"]
    fab_ver = target["fabric"]
    print(f"Building for Minecraft {mc_ver}...")
    
    gp_new = gp_content
    gp_new = re.sub(r"minecraft_version=.*", f"minecraft_version={mc_ver}", gp_new)
    gp_new = re.sub(r"fabric_version=.*", f"fabric_version={fab_ver}", gp_new)
    with open("gradle.properties", "w", encoding="utf-8") as f:
        f.write(gp_new)
        
    fmj_new = fmj_content
    fmj_new = re.sub(r'"minecraft":\s*".*"', f'"minecraft": "~{mc_ver}"', fmj_new)
    with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
        f.write(fmj_new)
        
    env = os.environ.copy()
    env["JAVA_HOME"] = r"d:\git\minecraft-mods\LitematicaEasyPlaceVariants\jdk-25"
    result = subprocess.run([r".\gradlew.bat", "clean", "build"], shell=True, env=env)
    if result.returncode != 0:
        print(f"Build failed for MC {mc_ver}!")
        exit(1)
        
    src_jar = f"build/libs/litematica-auto-ignite-1.0.0.jar"
    dest_jar = f"release/litematica-auto-ignite-1.0.0-mc{mc_ver}.jar"
    shutil.copy(src_jar, dest_jar)
    print(f"Successfully copied build to {dest_jar}")

gp_final = re.sub(r"minecraft_version=.*", "minecraft_version=26.1.2", gp_content)
gp_final = re.sub(r"fabric_version=.*", "fabric_version=0.149.0+26.1.2", gp_final)
with open("gradle.properties", "w", encoding="utf-8") as f:
    f.write(gp_final)

fmj_final = re.sub(r'"minecraft":\s*".*"', '"minecraft": ">=26.1"', fmj_content)
with open("src/main/resources/fabric.mod.json", "w", encoding="utf-8") as f:
    f.write(fmj_final)

print("All builds completed successfully!")
