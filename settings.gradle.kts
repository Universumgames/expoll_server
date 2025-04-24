rootProject.name = "de.universegame.expoll"

if (file("../expoll-kotlin-commons").exists()) {
    includeBuild("../expoll-kotlin-commons") // use composite build locally
}
