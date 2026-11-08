plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge", "forge")
    }

    // The source uses the newest names; these map them back for older versions.
    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
            replace("entity.animal.horse.", "entity.animal.equine.")
            replace("entity.npc.AbstractVillager", "entity.npc.villager.AbstractVillager")
            replace("entity.npc.Villager", "entity.npc.villager.Villager")
            replace("entity.boss.EnderDragonPart", "entity.boss.enderdragon.EnderDragonPart")
        }
        string(current.parsed >= "1.21.5") {
            replace("entity.animal.Sheep", "entity.animal.sheep.Sheep")
        }
    }
}
