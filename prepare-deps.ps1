param([Parameter(Mandatory = $true)][string]$ModsPath)
$ErrorActionPreference = 'Stop'
$dependencies = @(
    @{ Source = 'ldlib-forge-1.20.1-1.0.50.jar'; Target = 'ldlib-forge-1.20.1-1.0.50.jar'; Hash = '559D2A0CF8F77D7D6487B2C1620A8EE09E6BED94268BE6DF77CBA0F7D8AC5112' },
    @{ Source = 'Mekanism-1.20.1-10.4.16.80.jar'; Target = 'mekanism-1.20.1-10.4.16.80.jar'; Hash = '3B0D191FA4A45E662F9725991D76A30192F30186ABB2BA694B873B5F7B9358D2' },
    @{ Source = 'MekanismGenerators-1.20.1-10.4.16.80.jar'; Target = 'mekanism-generators-1.20.1-10.4.16.80.jar'; Hash = 'BE2D5B8553F8638CFABF491002F95CEF6ACEF210F627ADDB391F5AF713502512' },
    @{ Source = 'JustEnoughMekanismMultiblocks-1.20.1-4.27.jar'; Target = 'jemm-1.20.1-4.27.jar'; Hash = 'D4B228FB6DA4E6B75DAE68719B1B3FD3926561B6394E2ECC0643E93895D6B5B3' },
    # JEI for the JEI port; not hash-pinned yet, so any copy with this exact file name is accepted.
    @{ Source = 'jei-1.20.1-forge-15.74.0.230.jar'; Target = 'jei-1.20.1-forge-15.74.0.230.jar'; Hash = $null }
)
# Validate all sources before changing the local dependency directory.
foreach ($dependency in $dependencies) {
    $source = Join-Path $ModsPath $dependency.Source
    if (-not (Test-Path -LiteralPath $source)) { throw "Missing dependency: $($dependency.Source)" }
    if ($dependency.Hash -and (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $dependency.Hash) {
        throw "Dependency does not match the tested artifact: $($dependency.Source)"
    }
}
$dependencyDirectory = Join-Path $PSScriptRoot 'deps'
New-Item -ItemType Directory -Path $dependencyDirectory -Force | Out-Null
foreach ($dependency in $dependencies) {
    Copy-Item -LiteralPath (Join-Path $ModsPath $dependency.Source) -Destination (Join-Path $dependencyDirectory $dependency.Target) -Force
}
Write-Output "Prepared $($dependencies.Count) pinned development dependencies."
