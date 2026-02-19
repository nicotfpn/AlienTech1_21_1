    # AlienTech Mod - Development Lessons

    ## Session 2026-02-02

    ### Lesson 1: Fuel System Changes Require Full Search
    - When changing a fuel/resource type (e.g., Alloy → Graviton), search ALL files for references
    - Check: block entity logic, item validation, translation keys, messages, tooltips
    - Pattern: `grep -r "OLD_ITEM_NAME" src/` before considering done

    ### Lesson 2: Ground Crafting Needs Performance Consideration
    - Iterating all entities every tick is expensive
    - Solution: Use tick interval (CHECK_INTERVAL = 20) to reduce overhead
    - Alternative: Listen to item drop events instead of polling

    ### Lesson 3: Registration Helpers Improve Scaling
    - For mods with many items/blocks, helper methods reduce boilerplate
    - Keep helpers simple and composable
    - Document usage with examples in JavaDoc

    ### Lesson 4: Remove Unused Code When Removing Features
    - When a feature is removed (e.g., wireless charging), also delete:
    - The method implementation
    - Any GUI/screen indicators
    - Translation keys if any
    - Pattern: After removing call site, grep for method name to find orphans
