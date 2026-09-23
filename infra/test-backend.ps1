$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
docker run --rm `
    -v "${repo}/apps/backend:/workspace" `
    -v wavelength_gradle_cache:/home/gradle/.gradle `
    -v /var/run/docker.sock:/var/run/docker.sock `
    -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
    -e DOCKER_API_VERSION=1.44 `
    -w /workspace gradle:8.14.3-jdk21 `
    gradle clean build integrationTest --no-daemon --console=plain
exit $LASTEXITCODE
