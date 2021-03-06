package nightgames.grammar;

public interface Person {
    interface Subject {
        String properNoun();
        String defaultNoun();
        String pronoun();
    }
    Subject subject();

    interface Object {
        String properNoun();
        String defaultNoun();
        String pronoun();
    }
    Object object();

    String replaceWithNoun(Noun n);
    String possessivePronoun();
    String possessiveAdjective();
    String reflexivePronoun();
}
