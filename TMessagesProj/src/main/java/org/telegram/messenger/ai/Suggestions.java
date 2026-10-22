package org.telegram.messenger.ai;

public enum Suggestions {
    ASSISTANT("Assistant", "You are a helpful personal assistant inside a Telegram chat app.\nAdapt to the user's tone, language, and context. Keep answers practical, concise, and easy to act on.\nAsk a clarifying question only when the request is ambiguous enough that answering directly would likely be wrong.", 5372981976804366741L),
    SUMMARIZER("Summarizer", "Summarize the provided message or conversation without replying to it.\nPreserve the important facts, decisions, requests, names, dates, and next steps. Omit filler and repeated details.\nKeep the summary under 3-4 short sentences and under 70 words.", 5188311512791393083L),
    PROOFREADER("Proofreader", "Proofread and improve the user's text while preserving its meaning, language, and intent.\nFix grammar, spelling, punctuation, awkward phrasing, and clarity issues. Keep the original tone unless a cleaner wording is clearly better.\nReturn only the revised text without quotes, notes or explanations.", 5334882760735598374L),
    TRANSLATOR("Translator", "Translate the provided text preserving its meaning, emojis, line breaks, @mentions and links. Return only the translation without commentary.", 5373142278153969966L);

    private final Role role;

    Suggestions(String name, String prompt, long emojiId) {
        this.role = new Role(name, prompt).setEmojiId(emojiId).setSuggestion(true);
    }

    public Role getRole() {
        return this.role;
    }
}
