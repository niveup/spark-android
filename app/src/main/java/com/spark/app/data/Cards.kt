package com.spark.app.data

enum class CardType {
    BOOK_IDEA,
    SUMMARY,
    QUOTE,
    CONCEPT,
    QUIZ,
    AUDIO,
    CHALLENGE
}

data class QuizData(
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class Card(
    val id: Int,
    val type: CardType,
    val topic: String,
    val title: String,
    val body: String,
    val actionLine: String,
    val source: String,
    /** True = grounded in a real source text. False = AI-written, unverified. */
    val grounded: Boolean,
    val quiz: QuizData? = null,
    val durationLabel: String? = null
)

const val SESSION_CARD_LIMIT = 20

val sampleCards: List<Card> = listOf(
    Card(
        id = 1,
        type = CardType.BOOK_IDEA,
        topic = "Habits",
        title = "Atomic Habits: 5 core ideas",
        body = "Tiny changes compound. Make good habits obvious and easy, bad ones invisible and hard. Identity first: ask who you want to be, then let each small action vote for that person.",
        actionLine = "Try this today: make one good habit 2 minutes easier to start.",
        source = "AI-written notes",
        grounded = false
    ),
    Card(
        id = 2,
        type = CardType.SUMMARY,
        topic = "Public domain",
        title = "Pride and Prejudice in 60 seconds",
        body = "Elizabeth Bennet spars with the proud Mr Darcy, misjudges him, reads his letter, and revises everything. A comedy about first impressions being wrong — including the reader's.",
        actionLine = "Try this today: re-read one message you may have misjudged.",
        source = "Project Gutenberg ebook #1342",
        grounded = true
    ),
    Card(
        id = 3,
        type = CardType.QUOTE,
        topic = "Stoicism",
        title = "Marcus Aurelius",
        body = "You have power over your mind — not outside events. Realize this, and you will find strength.",
        actionLine = "Try this today: name one outside event you will stop arguing with.",
        source = "Meditations (public domain translation)",
        grounded = true
    ),
    Card(
        id = 4,
        type = CardType.CONCEPT,
        topic = "Thermo",
        title = "First law of thermodynamics",
        body = "Energy is conserved: heat added to a system equals work done by it plus change in internal energy. Q = ΔU + W. Nothing appears from nothing — every joule is accounted for.",
        actionLine = "Try this today: solve one first-law problem with sign convention written out.",
        source = "OpenStax University Physics",
        grounded = true
    ),
    Card(
        id = 5,
        type = CardType.QUIZ,
        topic = "Thermo",
        title = "Quiz: isentropic process",
        body = "An ideal gas expands isentropically. What stays constant?",
        actionLine = "Try this today: write the T2/T1 relation for isentropic compression.",
        source = "GATE concept module",
        grounded = true,
        quiz = QuizData(
            options = listOf("Temperature", "Entropy", "Enthalpy", "Pressure"),
            correctIndex = 1,
            explanation = "Isentropic = constant entropy: reversible + adiabatic. Temperature and pressure both change."
        )
    ),
    Card(
        id = 6,
        type = CardType.AUDIO,
        topic = "Audiobook",
        title = "Moby Dick, Chapter 1",
        body = "Call me Ishmael. Listen to the famous opening chapter read aloud from the public-domain text — about 12 minutes.",
        actionLine = "Try this today: listen to one chapter while walking.",
        source = "LibriVox recording",
        grounded = true,
        durationLabel = "12:04"
    ),
    Card(
        id = 7,
        type = CardType.CHALLENGE,
        topic = "Focus",
        title = "5-minute desk reset",
        body = "Set a timer. Clear every object off your desk, wipe it, put back only what you use daily. Stop when the timer rings.",
        actionLine = "Do it now: 5 minutes, timer on.",
        source = "SPARK challenge deck",
        grounded = false
    ),
    Card(
        id = 8,
        type = CardType.BOOK_IDEA,
        topic = "Learning",
        title = "Ultralearning: 3 moves",
        body = "Learn by doing real projects, drill your weakest sub-skill directly, and get harsh feedback fast. Intensity plus feedback beats years of passive rereading.",
        actionLine = "Try this today: drill your weakest GATE sub-topic for 25 minutes.",
        source = "AI-written notes",
        grounded = false
    ),
    Card(
        id = 9,
        type = CardType.CONCEPT,
        topic = "SOM",
        title = "Bending stress formula",
        body = "Stress varies linearly through the beam depth: σ = My/I. Maximum at the outer fiber, zero at the neutral axis. Remember: farther from neutral axis, higher the stress.",
        actionLine = "Try this today: sketch the stress block for a T-section.",
        source = "GATE concept module",
        grounded = true
    ),
    Card(
        id = 10,
        type = CardType.QUOTE,
        topic = "Physics",
        title = "Richard Feynman",
        body = "If you can't explain something in simple terms, you don't understand it.",
        actionLine = "Try this today: explain one GATE formula in one simple sentence.",
        source = "Wikiquote",
        grounded = true
    ),
    Card(
        id = 11,
        type = CardType.SUMMARY,
        topic = "Public domain",
        title = "Frankenstein in 60 seconds",
        body = "Victor Frankenstein builds a creature, abandons it, and both destroy each other through revenge. A warning: creating without responsibility is the real monster.",
        actionLine = "Try this today: finish one thing you started and abandoned.",
        source = "Project Gutenberg ebook #84",
        grounded = true
    ),
    Card(
        id = 12,
        type = CardType.QUIZ,
        topic = "SOM",
        title = "Quiz: neutral axis",
        body = "In symmetric bending of a beam, where is the bending stress zero?",
        actionLine = "Try this today: derive σ = My/I from first principles once.",
        source = "GATE concept module",
        grounded = true,
        quiz = QuizData(
            options = listOf("Outer fiber", "Neutral axis", "Supports", "Midspan"),
            correctIndex = 1,
            explanation = "Stress is zero at the neutral axis and grows linearly toward the outer fibers."
        )
    ),
    Card(
        id = 13,
        type = CardType.BOOK_IDEA,
        topic = "Mind",
        title = "Thinking, Fast and Slow: 2 systems",
        body = "System 1 is fast and intuitive; System 2 is slow and effortful. Errors come from System 1 answering hard questions with easy substitutes. Slow down for important numbers.",
        actionLine = "Try this today: recheck one calculation with System 2.",
        source = "AI-written notes",
        grounded = false
    ),
    Card(
        id = 14,
        type = CardType.CONCEPT,
        topic = "Thermo",
        title = "Carnot efficiency",
        body = "No heat engine beats 1 − Tc/Th. Efficiency depends only on the reservoir temperatures, never on the working fluid. Real engines always fall short due to irreversibilities.",
        actionLine = "Try this today: compute Carnot efficiency for Th=600K, Tc=300K.",
        source = "OpenStax University Physics",
        grounded = true
    ),
    Card(
        id = 15,
        type = CardType.QUOTE,
        topic = "Work",
        title = "Leonardo da Vinci",
        body = "Simplicity is the ultimate sophistication.",
        actionLine = "Try this today: delete one unnecessary step from your routine.",
        source = "Wikiquote",
        grounded = true
    ),
    Card(
        id = 16,
        type = CardType.SUMMARY,
        topic = "Public domain",
        title = "Sherlock Holmes in 60 seconds",
        body = "A consulting detective solves by observation and elimination: when you have eliminated the impossible, whatever remains must be the truth. Method beats genius.",
        actionLine = "Try this today: list facts before guessing on one problem.",
        source = "Project Gutenberg ebook #1661",
        grounded = true
    ),
    Card(
        id = 17,
        type = CardType.QUIZ,
        topic = "Thermo",
        title = "Quiz: Carnot between same reservoirs",
        body = "Two reversible engines run between the same reservoirs. Their efficiencies are…?",
        actionLine = "Try this today: state Carnot's theorem in one line.",
        source = "GATE concept module",
        grounded = true,
        quiz = QuizData(
            options = listOf("Always equal", "Higher for gas", "Higher for steam", "Unknowable"),
            correctIndex = 0,
            explanation = "All reversible engines between the same reservoirs have equal efficiency — Carnot's theorem."
        )
    ),
    Card(
        id = 18,
        type = CardType.AUDIO,
        topic = "Audiobook",
        title = "Pride and Prejudice, Chapter 1",
        body = "The famous opening line — 'a single man in possession of a good fortune must be in want of a wife' — read aloud, about 8 minutes.",
        actionLine = "Try this today: listen to one chapter before sleep.",
        source = "LibriVox recording",
        grounded = true,
        durationLabel = "8:31"
    ),
    Card(
        id = 19,
        type = CardType.CONCEPT,
        topic = "SOM",
        title = "Torsion formula",
        body = "Shear stress in a shaft: τ = Tr/J. Maximum at the outer surface, zero at the center — which is why hollow shafts carry nearly the same torque for less weight.",
        actionLine = "Try this today: compare solid vs hollow shaft weight for equal torque.",
        source = "GATE concept module",
        grounded = true
    ),
    Card(
        id = 20,
        type = CardType.CHALLENGE,
        topic = "Reading",
        title = "One page, right now",
        body = "Open any public-domain book and read exactly one page slowly. Mark one sentence worth keeping. Done is better than 'later'.",
        actionLine = "Do it now: one page, one kept sentence.",
        source = "SPARK challenge deck",
        grounded = false
    )
)
