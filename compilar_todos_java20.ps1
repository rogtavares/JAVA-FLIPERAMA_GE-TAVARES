# Script PowerShell para compilar e rodar todos os projetos Java (Java 11)
# Altere o caminho do JAVA_HOME se necessário
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# Lista de projetos (adicione mais se necessário)
$projetos = @(
    "Ge_01_Telejogo78",
    "Ge_02_ Space_BarBiju",
    "Ge_03_Tetris_Otica_Laranjal",
    "Ge_05_Asteroids_bar_zecareca",
    "Ge_06_Balsa_Capela Quebrada"
)

foreach ($proj in $projetos) {
    $src = Join-Path $proj 'src'
    if (Test-Path $src) {
        Write-Host "Compilando $proj..."
        Push-Location $src
        Remove-Item *.class -Recurse -Force -ErrorAction SilentlyContinue
        javac -version
        javac -d . (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Compilado com sucesso: $proj"
        } else {
            Write-Host "Erro ao compilar: $proj"
        }
        Pop-Location
    }
}

# Para rodar um projeto, descomente e ajuste a linha abaixo:
# cd "Ge_06_Balsa_Capela Quebrada\src"
# java br.com.mvbos.lgj.Jogo
