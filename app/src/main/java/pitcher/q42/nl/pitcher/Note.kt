package pitcher.q42.nl.pitcher

enum class Note(val range: ClosedFloatingPointRange<Float>, val label: String) {

    A(100.00F..123.47F, "A"),
    B(123.47F..130.81F, "B"),
    C(130.81F..146.83F, "C"),
    D(146.83F..164.81F, "D"),
    E(164.81F..174.61F, "E"),
    F(174.61F..185.00F, "F"),
    G(185F..196.00F, "G");
}