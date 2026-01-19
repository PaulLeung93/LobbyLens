package io.github.paulleung93.lobbylens.util

object VisionConstants {
    // Entities to ignore (generic terms that match too broadly)
    val IGNORED_ENTITIES = listOf(
        "United States", "Politics", "Government", "Society",
        "Public Speaking", "Event", "Official", "Businessperson",
        "Spokesperson", "Chairperson", "Senator", "Representative",
        "Computer", "Computer Keyboard", "Keyboard", "Mouse", "Computer mouse",
        "Screen", "Monitor", "Laptop", "MacBook", "Tablet", "USB",
        "DisplayLink", "Wireless keyboard"
    )

    // Common nickname mappings
    val NICKNAME_MAP = mapOf(
        "Chuck" to "Charles",
        "Bill" to "William",
        "Bob" to "Robert",
        "Dick" to "Richard",
        "Jim" to "James",
        "Mike" to "Michael",
        "Tom" to "Thomas",
        "Joe" to "Joseph",
        "Tim" to "Timothy",
        "Dan" to "Daniel",
        "Dave" to "David",
        "Ted" to "Edward",
        "Tony" to "Anthony",
        "Bernie" to "Bernard",
        "Beth" to "Elizabeth",
        "Liz" to "Elizabeth",
        "Katie" to "Katherine",
        "Kate" to "Katherine",
        "Chris" to "Christopher",
        "Matt" to "Matthew",
        "Alex" to "Alexander",
        "Andy" to "Andrew",
        "Greg" to "Gregory",
        "Steve" to "Steven",
        "Pat" to "Patricia"
    )
}
