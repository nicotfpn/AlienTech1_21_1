import os
import json

'''
=== ZERO-ERROR PATCHOULI PROTOCOL ===
CHECK THIS LIST EVERY TIME A NEW PAGE OR TIER IS ADDED:
1. [DIRECTORY STRUCTURE]: Is `use_resource_pack` true or false? 
   -> If TRUE (1.20+ format): Files MUST generate in `assets/[mod]/patchouli_books/[book]/en_us`.
   -> If FALSE (Legacy format): Files MUST generate in `data/[mod]/patchouli_books/[book]/en_us`.
2. [RECIPE NAMESPACES]: Do all `patchouli:crafting` recipe IDs exactly match the JSON filenames inside `data/alientech/recipe/`?
3. [CUSTOM RECIPE HANDLERS]: Are custom machine recipes (like Primal Catalyst processing) using `patchouli:spotlight` instead of `patchouli:crafting` so the game doesn't crash trying to parse them as vanilla crafting tables?
4. [MULTIBLOCK CENTERING]: In `pattern` arrays, is the `0` (Core block) located in the exact mathematical center of the grid? (e.g. at [2][2] in a 5x5, or [4][4] in a 9x9).
5. [MISSING ASSETS CHECK]: Do ALL block characters in the `mapping` section have physical blockstate, model, AND `.png` texture files inside `assets/alientech/`?
====================================
'''

BASE_DIR = r"c:\Users\Pichau\IdeaProjects\AlienTech1_21_1\src\main\resources\assets\alientech\patchouli_books\the_archives\en_us"

def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)

ensure_dir(os.path.join(BASE_DIR, "categories"))

categories = {
    "machines": {
        "name": "Alien Machinery",
        "description": "Fragmented logs detail these monolithic structures. Their purpose seems to revolve around harnessing universal $(alien)entropy$() and cosmic energy.",
        "icon": "alientech:pyramid_core",
        "sortnum": 0
    },
    "materials": {
        "name": "Exotic Materials",
        "description": "Strange matter that defies typical physics. Proceed with extreme caution; some of these substances are violently unstable.",
        "icon": "alientech:graviton",
        "sortnum": 1
    },
    "tools": {
        "name": "Relics & Tools",
        "description": "Artifacts of immense, terrifying power. The localized texts claim they were wielded by the Architects themselves.",
        "icon": "alientech:horus_eye_activated",
        "sortnum": 2
    }
}

for k, v in categories.items():
    with open(os.path.join(BASE_DIR, "categories", f"{k}.json"), "w", encoding="utf-8") as f:
        json.dump(v, f, indent=4)

entries = {
    "machines/pyramid_core": {
        "name": "Pyramid Core",
        "icon": "alientech:pyramid_core",
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The absolute pinnacle of their engineering. The $(item)Pyramid Core$() does not generate power itself, but rather $(alien)resonates$() with reality, broadcasting a massive efficiency boost to nearby $(l:patchouli:machines/quantum_vacuum_turbine)Quantum Vacuum Turbines$(/l)."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:pyramid_core"
            },
            {
                "type": "patchouli:text",
                "title": "Activation",
                "text": "The Core remains dormant until built into a massive pyramidal structure. Once built, it must be ignited by consuming an $(l:patchouli:materials/alloys_disks)Inertial Stability Alloy$(/l) or touched by an $(l:patchouli:tools/ancient_ankh)Ancient Ankh$(/l)."
            },
            {
                "type": "patchouli:multiblock",
                "name": "Pyramid Tier 1",
                "multiblock": {
                    "pattern": [
                        ["     ", "     ", "  0  ", "     ", "     "],
                        ["     ", " GGG ", " GGG ", " GGG ", "     "],
                        ["AAAAA", "AAAAA", "AAAAA", "AAAAA", "AAAAA"]
                    ],
                    "mapping": {
                        "0": "alientech:pyramid_core",
                        "G": "minecraft:gold_block",
                        "A": "alientech:alien_pyramid_casing"
                    }
                },
                "text": "The Tier 1 Structure requires a 3x3 of $(gold)Gold Blocks$() directly beneath the Core, followed by a 5x5 of $(alien)Alien Pyramid Casing$(). Grants a modest boost."
            },
            {
                "type": "patchouli:multiblock",
                "name": "Pyramid Tier 2",
                "multiblock": {
                    "pattern": [
                        ["       ", "       ", "       ", "   0   ", "       ", "       ", "       "],
                        ["       ", "       ", "  GGG  ", "  GGG  ", "  GGG  ", "       ", "       "],
                        ["       ", " AAAAA ", " AAAAA ", " AAAAA ", " AAAAA ", " AAAAA ", "       "],
                        ["AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA", "AAAAAAA"]
                    ],
                    "mapping": {
                        "0": "alientech:pyramid_core",
                        "G": "minecraft:gold_block",
                        "A": "alientech:alien_pyramid_casing"
                    }
                },
                "text": "By adding another layer beneath of 7x7 $(alien)Alien Pyramid Casing$(), the Core enters Tier 2 resonance, significantly increasing the multiplier."
            },
            {
                "type": "patchouli:multiblock",
                "name": "Pyramid Tier 3",
                "multiblock": {
                    "pattern": [
                        ["         ", "         ", "         ", "         ", "    0    ", "         ", "         ", "         ", "         "],
                        ["         ", "         ", "         ", "   GGG   ", "   GGG   ", "   GGG   ", "         ", "         ", "         "],
                        ["         ", "         ", "  AAAAA  ", "  AAAAA  ", "  AAAAA  ", "  AAAAA  ", "  AAAAA  ", "         ", "         "],
                        ["         ", " AAAAAAA ", " AAAAAAA ", " AAAAAAA ", " AAAAAAA ", " AAAAAAA ", " AAAAAAA ", " AAAAAAA ", "         "],
                        ["AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA"]
                    ],
                    "mapping": {
                        "0": "alientech:pyramid_core",
                        "G": "minecraft:gold_block",
                        "A": "alientech:alien_pyramid_casing"
                    }
                },
                "text": "The ultimate stable configuration. A final 9x9 base of $(alien)Alien Pyramid Casing$() maximizes the output of all connected Vacuum Turbines to terrifying levels."
            }
        ]
    },
    "machines/ancient_charger": {
        "name": "Ancient Charger",
        "icon": "alientech:ancient_charger",
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "An elegant mechanism designed to draw $(energy)cosmic energy$() directly from the atmosphere. By placing an energy-compatible item inside, the $(item)Ancient Charger$() will slowly but infinitely replenish its reserves without fuel."
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
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A colossal energy repository. The $(item)Ancient Battery$() can store millions of FE. $(br2)It interfaces with adjacent cables and machines seamlessly, featuring an internal GUI to both inject and extract charge manually from items."
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
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A mythical variant of the Ancient Battery that taps straight into the universe's fabric, providing $(energy)limitless power$() to all connected structures. $(br2)It cannot be obtained by mortals."
            }
        ]
    },
    "machines/primal_catalyst": {
        "name": "Primal Catalyst",
        "icon": "alientech:primal_catalyst",
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A highly sophisticated forge that operates at the molecular level. The $(item)Primal Catalyst$() must be fed exactly 3 distinct specific ingredients and extreme amounts of $(energy)Energy$() to synthesize legendary artifacts."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:primal_catalyst"
            },
            {
                "type": "patchouli:text",
                "title": "Usage Warning",
                "text": "Currently, its primary known usage is forging the devastatingly dense $(l:patchouli:materials/alloys_disks)Inertial Stability Alloy$(). Attempting unauthorized recipes will simply freeze the processing matrix."
            }
        ]
    },
    "machines/quantum_vacuum_turbine": {
        "name": "Quantum Vacuum Turbine",
        "icon": "alientech:quantum_vacuum_turbine",
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "By tearing microscopic holes into the quantum sub-realm, this turbine harvests $(energy)zero-point energy$(). Running solo, it is slow and inefficient. But when bathed in the resonant fields of an active $(item)Pyramid Core$(), it generates staggering voltage."
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
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A horrifying machinery constructed to forcefully rip life force from living beings and convert it into pure $(alien)Entropy Biomass$(). It requires a multi-block chamber led by a $(item)Controller$()."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:decay_chamber_controller",
                "text": "The Controller manages insertions and outputs."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:decay_chamber",
                "text": "These form the walls of the terrifying prison."
            },
            {
                "type": "patchouli:multiblock",
                "name": "Decay Chamber Setup",
                "multiblock": {
                    "pattern": [
                        [ " C ", "   ", "   " ],
                        [ " C ", " 0 ", "   " ]
                    ],
                    "mapping": {
                        "0": "alientech:decay_chamber_controller",
                        "C": "alientech:decay_chamber"
                    }
                },
                "text": "To function, the Controller requires at least one 2-block high pillar of $(item)Decay Chambers$() positioned directly horizontally adjacent to it."
            }
        ]
    },
    "machines/evolution_chamber": {
        "name": "Evolution Chamber",
        "icon": "alientech:evolution_chamber",
        "category": "alientech:machines",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The antithesis to the Decay Chamber. By forcefully injecting $(alien)Entropy$() into lesser materials, this chamber forces unnatural, rapid mutation into hyper-evolved exotic states."
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
        "category": "alientech:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Substrates form the chemical backbone of Alien technology, bridging the gap between primitive Overworld matter and extraterrestrial materials."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:exotic_substrate",
                "text": "$(alien)Exotic Substrate$() - The first step."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:concentrated_substrate",
                "text": "$(alien)Concentrated Substrate$() - Highly refined."
            }
        ]
    },
    "materials/neutrion": {
        "name": "Neutrion",
        "icon": "alientech:neutrion_ingot",
        "category": "alientech:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A metal so dense it generates its own microscopic gravity wells. Forged from raw neutrion blasted in ultra-high temperature furnaces."
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
        "category": "alientech:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "Sub-atomic particles exhibiting massive gravity distortion fields. They decay so rapidly they must be immediately encapsulated."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:graviton"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:graviton_contained",
                "text": "$(item)Contained Graviton$() - Stabilized inside a fortified glass housing to prevent spontaneous black holes."
            }
        ]
    },
    "materials/alloys_disks": {
        "name": "Alloys & Disks",
        "icon": "alientech:inertial_stability_alloy",
        "category": "alientech:materials",
        "pages": [
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:gravion_disk",
                "text": "$(alien)Gravion Disk$(): Can store immense kinetic shockwaves."
            },
            {
                "type": "patchouli:spotlight",
                "item": "alientech:inertial_stability_alloy",
                "text": "Forged in the Primal Catalyst via:$(br2)• $(alien)Concentrated Substrate$()$(br2)• $(alien)Neutrion Ingot$()$(br2)• $(alien)Graviton$()$(br2)$(br2)Provides the $(gold)Pyramid Core$() supreme stabilization."
            }
        ]
    },
    "materials/rainbow_captured": {
        "name": "Captured Rainbow",
        "icon": "alientech:rainbow_captured",
        "category": "alientech:materials",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "By refracting light through perfectly cut prismatic dimensions, a literal localized rainbow can be captured inside a jar."
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
        "category": "alientech:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A relic of pure divinity, vibrating with a frequency that nullifies entropy. It is capable of activating the $(item)Pyramid Core$() structure infinitely, without being consumed in the process like primitive alloys."
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
        "category": "alientech:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The sharpest blade to ever physically manifest in this dimension. Imbued with the destructive hunting forces of the Ancients, it severs molecular bonds rather than just cutting flesh."
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
        "category": "alientech:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "A terrifying device that weaponizes spatial geometry. Upon striking any living entity, the target is shrunk and folded into a pocket quantum dimension inside the prison. $(br2)Shift-Right click any block to violently violently expel them back into our reality."
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
        "category": "alientech:tools",
        "pages": [
            {
                "type": "patchouli:text",
                "text": "The silent observer of all things. Currently dormant, but when charged and infused with Neutrion, it awakens to become the $(item)Activated Eye of Horus$()."
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:horus_eye"
            },
            {
                "type": "patchouli:crafting",
                "recipe": "alientech:horus_eye_activated",
                "text": "Once awakened, it grants abilities entirely unhindered by conventional physics, acting as a direct link to the \"Archives\" themselves."
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

print("Perfect Patchouli generated.")
