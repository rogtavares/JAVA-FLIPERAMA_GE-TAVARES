# Script PowerShell para compilar todos os projetos Java do Fliperama
# Usa o JDK ja disponivel no PATH do sistema (nao fixa mais um caminho de JDK inexistente)

$ErrorActionPreference = "Stop"

Write-Host "Usando JDK:" -ForegroundColor Cyan
javac -version

$projetos = @(
    "Ge_01_Telejogo78",
    "Ge_02_ Space_BarBiju",
    "Ge_03_Tetris_Otica_Laranjal",
    "Ge_04_Pacman_tavares",
    "Ge_05_Asteroids_bar_zecareca",
    "Ge_06_Balsa_Capela Quebrada"
)

$falhou = @()

foreach ($proj in $projetos) {
    $src = Join-Path $proj 'src'
    $bin = Join-Path $proj 'bin'

    if (Test-Path $src) {
        Write-Host "`nCompilando $proj..." -ForegroundColor Yellow

        if (-not (Test-Path $bin)) {
            New-Item -ItemType Directory -Path $bin | Out-Null
        }

        $fontes = Get-ChildItem -Path $src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
        javac -encoding UTF-8 -d $bin $fontes

        if ($LASTEXITCODE -eq 0) {
            Write-Host "Compilado com sucesso: $proj" -ForegroundColor Green
        } else {
            Write-Host "Erro ao compilar: $proj" -ForegroundColor Red
            $falhou += $proj
        }
    } else {
        Write-Host "Pasta 'src' nao encontrada para $proj, pulando." -ForegroundColor DarkYellow
    }
}

Write-Host "`n===================================="
if ($falhou.Count -eq 0) {
    Write-Host "Todos os projetos compilaram com sucesso." -ForegroundColor Green
} else {
    Write-Host "Projetos com erro de compilacao:" -ForegroundColor Red
    $falhou | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
}
