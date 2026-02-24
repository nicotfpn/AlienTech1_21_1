import os
import json
import os.path

BASE_DIR = r"c:\Users\Pichau\IdeaProjects\AlienTech1_21_1\src\main\resources\data\alientech\patchouli_books\the_archives\en_us"

def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

# Categories
ensure_dir(os.path.join(BASE_DIR, "categories"))

categories = {
    "machines": {
        "name": "Alien Machinery",
        "description": "Information regarding the $(alien)ancient machinery$() left behind by the Architects.",
        "icon": "alientech:pyramid_core",
        "sortnum": 0
    },
    "materials": {
        "name": "Exotic Materials",
        "description": "Strange matter, dense alloys, and mysterious substances found around the structures.",
        "icon": "alientech:graviton",
        "sortnum": 1
    },
    "tools": {
        "name": "Relics & Tools",
        "description": "Artifacts of immense power that can be wielded by the worthy.",
        "icon": "alientech:horus_eye_activated",
        "sortnum": 2
    }
}

for k, v in categories.items():
    with open(os.path.join(BASE_DIR, "categories", f"{k}.json"), "w", encoding="utf-8") as f:
        json.dump(v, f, indent=4)

# Entries
entries = {
    "machines/ancient_charger": {
        "name": "Ancient Charger",
        "icon": "alientech:ancient_charger",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The $(item)Ancient Charger$() passively collects ambient cosmic energy to charge any compatible energy-holding items placed inside it."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:ancient_charger"
            }
        ]
    },
    "machines/ancient_battery": {
        "name": "Ancient Battery",
        "icon": "alientech:ancient_battery",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A massive energy repository. The $(item)Ancient Battery$() can store vast amounts of FE and interact with nearby devices."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:ancient_battery"
            }
        ]
    },
    "machines/creative_ancient_battery": {
        "name": "Creative Ancient Battery",
        "icon": "alientech:creative_ancient_battery",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A mythical variant of the Ancient Battery that provides $(energy)limitless power$() to all adjacent blocks. It cannot be obtained through normal means (Creative Mode only)."
            }
        ]
    },
    "machines/primal_catalyst": {
        "name": "Primal Catalyst",
        "icon": "alientech:primal_catalyst",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The $(item)Primal Catalyst$() combines 3 distinct items along with highly concentrated energy to form legendary artifacts, such as the Inertial Stability Alloy."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:primal_catalyst"
            },
            {
                "type": "patchouli:text",
                "title": "Operation",
                "text": "You must supply exactly 3 specific ingredients and significant FE power. Its primary known recipe yields the $(item)Inertial Stability Alloy$()."
            }
        ]
    },
    "machines/quantum_vacuum_turbine": {
        "name": "Quantum Vacuum Turbine",
        "icon": "alientech:quantum_vacuum_turbine",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A potent energy generator slowly harvesting zero-point energy. It can run indefinitely, but it's slow. If linked to an active $(item)Pyramid Core$(), its production multiplies dramatically."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:quantum_vacuum_turbine"
            }
        ]
    },
    "machines/decay_chamber": {
        "name": "Decay Chamber",
        "icon": "alientech:decay_chamber_controller",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The $(item)Decay Chamber Controller$() drives the structure that extracts Entropy from captured mobs and blocks. Requires multiple $(item)Decay Chamber$() blocks attached to function properly."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:decay_chamber_controller",
                "text": "The Controller"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:decay_chamber",
                "text": "Decay Chamber structure blocks"
            }
        ]
    },
    "machines/evolution_chamber": {
        "name": "Evolution Chamber",
        "icon": "alientech:evolution_chamber",
        "category": "patchouli:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Harnessing the Entropy collected from the Decay Chamber, the $(item)Evolution Chamber$() is used to forcefully evolve or mutate objects into higher states."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:evolution_chamber"
            }
        ]
    },
    "materials/substrates": {
        "name": "Substrates",
        "icon": "alientech:concentrated_substrate",
        "category": "patchouli:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Substrates are foundational chemical matter processing fragments used to bridge normal materials with Alien Tech."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:exotic_substrate",
                "text": "Exotic Substrate"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:concentrated_substrate",
                "text": "Concentrated Substrate"
            }
        ]
    },
    "materials/neutrion": {
        "name": "Neutrion",
        "icon": "alientech:neutrion_ingot",
        "category": "patchouli:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A dark, highly dense metal forged from exotic ores in high-powered blasters."
            },
            {
                "type": "patchouli:smelting",
                "recipe": "alientech:neutrion_ingot_blasting"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:neutrion_block"
            }
        ]
    },
    "materials/graviton": {
        "name": "Graviton Matters",
        "icon": "alientech:graviton",
        "category": "patchouli:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Particles that exhibit gravity distortion fields. They naturally decay into $(item)Decaying Gravitons$() and must be contained quickly in a $(item)Contained Graviton$() unit."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:graviton"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:graviton_contained",
                "text": "Stabilized inside a glass housing."
            }
        ]
    },
    "materials/alloys_disks": {
        "name": "Alloys & Disks",
        "icon": "alientech:inertial_stability_alloy",
        "category": "patchouli:materials",
        "pages": [
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:gravion_disk",
                "text": "Stores intense kinetic energy."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:primal_catalyst/inertial_stability_alloy",
                "text": "The key to unlocking the true potential of the Pyramid Core, granting it supreme stability."
            }
        ]
    },
    "materials/rainbow_captured": {
        "name": "Captured Rainbow",
        "icon": "alientech:rainbow_captured",
        "category": "patchouli:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Light physically bent and trapped within a prism."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:rainbow_captured"
            }
        ]
    },
    "tools/ancient_ankh": {
        "name": "Ancient Ankh",
        "icon": "alientech:ancient_ankh",
        "category": "patchouli:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A relic of pure divinity. Capable of activating the $(item)Pyramid Core$() structure infinitely, without being consumed like the Inertial Stability Alloy."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:ancient_ankh"
            }
        ]
    },
    "tools/pharaoh_sword": {
        "name": "Pharaoh Sword",
        "icon": "alientech:pharaoh_sword",
        "category": "patchouli:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The sharpest blade known to exist, imbued with the destructive forces of the Ancients."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:pharaoh_sword"
            }
        ]
    },
    "tools/pocket_dimension": {
        "name": "Pocket Dimensional Prison",
        "icon": "alientech:pocket_dimensional_prison",
        "category": "patchouli:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Can capture any living entity by hitting them. The entity shrinks into the quantum realm inside the prison. Shift-Right click to release them back."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:pocket_dimensional_prison"
            }
        ]
    },
    "tools/horus_eye": {
        "name": "Eye of Horus",
        "icon": "alientech:horus_eye",
        "category": "patchouli:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The observer of all things. When charged and infused with Neutrion, it awakens to become the $(item)Activated Eye of Horus$(), granting abilities unhindered by physics."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:horus_eye"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:horus_eye_activated"
            }
        ]
    }
}

ensure_dir(os.path.join(BASE_DIR, "entries", "machines"))
ensure_dir(os.path.join(BASE_DIR, "entries", "materials"))
ensure_dir(os.path.join(BASE_DIR, "entries", "tools"))

for k, v in entries.items():
    with open(os.path.join(BASE_DIR, "entries", f"{k}.json"), "w", encoding="utf-8") as f:
        json.dump(v, f, indent=4)

print("Patchouli generated.")
