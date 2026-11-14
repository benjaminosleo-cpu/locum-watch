package com.benjaminosleo.locumwatch;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Decides whether a WhatsApp message in the locum group is a locum request.
 *
 * Tuned and tested against the ENT Locums Group history (Apr 2025 - Nov 2026):
 *  - Karen (ENT rota): any shift offer or request for cover.
 *  - Anyone else: stricter - must be for today / tonight / tomorrow / this weekend,
 *    mention a role or shift, and not be a reply or about their own shift or a swap.
 * Swaps, "gone / taken / left" updates, rota admin, jobs and chat are ignored.
 *
 * Pure Java (no Android) so it can be unit-tested against the chat history.
 */
public final class LocumFilter {
    private LocumFilter() {}

    // ---------- Karen's messages ----------
    private static final Pattern K_NOT = Pattern.compile(
        "(swap|\\bgone\\b|gobe|taken|\\bleft\\b|cancell?ed|induction|\\bjobs?\\b|\\bposts?\\b|advert|apply|remember|hive|seen\\b|rates? below|new rates|all yours|\\byours\\b|covered(?!\\s*please)|message was deleted)");
    private static final Pattern K_JUST = Pattern.compile("^\\s*just\\b");
    private static final Pattern K_SHIFT = Pattern.compile(
        "\\b(nights?|long ?b?days?|days?|on ?calls?|oncall|ooh|locums?|shifts?|5\\s*[-=]\\s*8(?:pm)?|8\\s*[-=]\\s*[58]|sho|theatres?|rac|clinics?|cover|assist|mri|wythenshawe|wyth?|wyt|north|rmch)\\b");
    private static final Pattern K_WHEN = Pattern.compile(
        "\\b(today|tonight|tomorrow|tomm?orr?ow|tommrrow|weekend|mon(day)?|tues?(day)?|wed(nesday)?|thu(rs?)?(day)?|fri(day)?|sat(urday)?|sun(day)?|\\d{1,2}(st|nd|rd|th))\\b");
    private static final Pattern K_ASK = Pattern.compile(
        "(any ?one|anybody|who'?s free|whos free|who free|who(?:'?s)? available|free\\b|availa?b?le?|availble|availableto|needed|needs?\\b|requires?|require\\b|looking for|can (?:some|any)one|help)");
    private static final Pattern ANYONE = Pattern.compile("any ?one");

    // ---------- everyone else ----------
    private static final Pattern O_NOT = Pattern.compile(
        "(swap|\\bmy\\b|fingers crossed|\\bgone\\b|taken|covered now|all covered|filled|cancell?ed|teaching|induction|(?:any ?one|anybody) know (?:how|where|if|what)|where is|anyone seen|message was deleted)");
    private static final Pattern O_REPLY = Pattern.compile(
        "^\\s*(sorry|apologies|apols|thanks|thank you|yes|no\\b|ok\\b|i\\b|i'm|im\\b|i’m|i am|i can|can do|hi karen|karen)");
    private static final Pattern O_URGENT = Pattern.compile(
        "\\b(today|tonight'?s?|tomorrow|tomm?orr?ow|this (evening|morning|afternoon|weekend|mon(day)?|tues?(day)?|wed(nesday)?|thu(rs)?(day)?|fri(day)?|sat(urday)?|sun(day)?)|weekend)\\b");
    private static final Pattern O_ROLE = Pattern.compile(
        "\\b(on ?calls?|oncalls?|sho|locums?|shifts?|cover|nights?|long days?|5\\s*-\\s*8|8\\s*-\\s*[58]|rota|gap|theatre|resident)\\b");
    private static final Pattern O_REQ = Pattern.compile(
        "(any ?one|anybody|looking for|needed|need\\b|gap in|available|can (?:some|any)one|cover the)");
    private static final Pattern O_LOOKING = Pattern.compile("looking for some(one|body)");

    public static boolean isKaren(String sender) {
        // "Karen (ENT rota)" normally; still match if the contact is renamed or shows as "~Karen".
        return lower(sender).contains("karen");
    }

    public static boolean isLocumRequest(String text) {
        String t = lower(text);
        if (K_NOT.matcher(t).find() || K_JUST.matcher(t).find()) return false;
        boolean shift = K_SHIFT.matcher(t).find();
        boolean when = K_WHEN.matcher(t).find();
        boolean ask = K_ASK.matcher(t).find();
        if (shift && ask) return true;                         // "Long day needed MRI"
        if (shift && when && t.length() < 120) return true;    // "Nights 26th 27th 28th June MRI"
        return ask && when && ANYONE.matcher(t).find();        // "Anyone available Tues"
    }

    public static boolean isOtherMemberRequest(String text) {
        String t = lower(text);
        if (O_NOT.matcher(t).find() || O_REPLY.matcher(t).find()) return false;
        if (!O_URGENT.matcher(t).find()) return false;
        if (O_LOOKING.matcher(t).find()) return true;
        return O_ROLE.matcher(t).find() && O_REQ.matcher(t).find();
    }

    public static boolean isAlarm(String sender, String text) {
        return isKaren(sender) ? isLocumRequest(text) : isOtherMemberRequest(text);
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
