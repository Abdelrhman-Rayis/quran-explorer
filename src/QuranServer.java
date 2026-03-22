import com.sun.net.httpserver.*;
import org.jqurantree.orthography.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class QuranServer {

    private static final int PORT = 9090;
    private static final String WEB_DIR = "web";

    // key: "ch:v:t" → list of segments (each segment is a feature map)
    private static final Map<String, List<Map<String, String>>> MORPH = new LinkedHashMap<>();

    private static final String[] CHAPTER_NAMES = {
        "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa", "Al-Ma'idah",
        "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
        "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr",
        "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Ta-Ha",
        "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan",
        "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir",
        "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
        "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah",
        "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
        "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman",
        "Al-Waqi'ah", "Al-Hadid", "Al-Mujadila", "Al-Hashr", "Al-Mumtahanah",
        "As-Saf", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq",
        "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
        "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah",
        "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "Abasa",
        "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj",
        "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin",
        "Al-Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-Adiyat",
        "Al-Qari'ah", "At-Takathur", "Al-Asr", "Al-Humazah", "Al-Fil",
        "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
        "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"
    };

    // Human-readable POS labels
    private static final Map<String, String> POS_LABELS = new LinkedHashMap<>();
    static {
        POS_LABELS.put("N",    "Noun");
        POS_LABELS.put("PN",   "Proper Noun");
        POS_LABELS.put("V",    "Verb");
        POS_LABELS.put("ADJ",  "Adjective");
        POS_LABELS.put("P",    "Preposition");
        POS_LABELS.put("PREP", "Preposition");
        POS_LABELS.put("CONJ", "Conjunction");
        POS_LABELS.put("CCONJ","Coord. Conjunction");
        POS_LABELS.put("DET",  "Definite Article");
        POS_LABELS.put("PRON", "Pronoun");
        POS_LABELS.put("REL",  "Relative Pronoun");
        POS_LABELS.put("DEM",  "Demonstrative");
        POS_LABELS.put("PRO",  "Interrogative");
        POS_LABELS.put("NEG",  "Negative Particle");
        POS_LABELS.put("PART", "Particle");
        POS_LABELS.put("INTJ", "Interjection");
        POS_LABELS.put("VOC",  "Vocative");
        POS_LABELS.put("COND", "Conditional");
        POS_LABELS.put("EMPH", "Emphatic");
        POS_LABELS.put("IMPV", "Imperative Particle");
        POS_LABELS.put("PREV", "Verbal Prefix");
        POS_LABELS.put("RSLT", "Result Particle");
        POS_LABELS.put("SUB",  "Subordinating Conj.");
        POS_LABELS.put("EXP",  "Exceptive");
        POS_LABELS.put("EXPL", "Explanation");
        POS_LABELS.put("CERT", "Certainty");
        POS_LABELS.put("INCEP","Inceptive");
        POS_LABELS.put("T",    "Time Adverb");
        POS_LABELS.put("LOC",  "Location Adverb");
        POS_LABELS.put("AMD",  "Amendment");
        POS_LABELS.put("ANS",  "Answer Particle");
        POS_LABELS.put("AVR",  "Aversion");
        POS_LABELS.put("COM",  "Comitative");
        POS_LABELS.put("EQ",   "Equalization");
        POS_LABELS.put("FUT",  "Future Particle");
        POS_LABELS.put("INC",  "Inceptive");
        POS_LABELS.put("INT",  "Interpretation");
        POS_LABELS.put("PERF", "Perfective");
        POS_LABELS.put("REM",  "Resumption");
        POS_LABELS.put("RET",  "Retraction");
        POS_LABELS.put("SUR",  "Surprise");
        POS_LABELS.put("AMP",  "Amplification");
        POS_LABELS.put("SUSP", "Supplementary");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Loading Quran data...");
        int chapterCount = Document.getChapterCount();
        System.out.println("Loaded " + chapterCount + " chapters, " +
            Document.getVerseCount() + " verses, " +
            Document.getTokenCount() + " tokens.");

        System.out.println("Loading morphology corpus...");
        loadCorpus();
        System.out.println("Loaded " + MORPH.size() + " token morphology entries.");

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/chapters",  QuranServer::handleChapters);
        server.createContext("/api/chapter/",  QuranServer::handleChapter);
        server.createContext("/api/verse/",    QuranServer::handleVerse);
        server.createContext("/api/morph/",    QuranServer::handleMorph);
        server.createContext("/api/search",    QuranServer::handleSearch);
        server.createContext("/",              QuranServer::handleStatic);
        server.setExecutor(null);
        server.start();
        System.out.println("Quran Explorer running at http://localhost:" + PORT);
    }

    // ── Corpus loader ─────────────────────────────────────────────────────────
    private static void loadCorpus() throws IOException {
        // Try multiple locations
        String[] candidates = {
            "corpus.txt",
            "../quranic-corpus-morphology-0.4.txt",
            "quranic-corpus-morphology-0.4.txt"
        };
        File corpusFile = null;
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) { corpusFile = f; break; }
        }
        if (corpusFile == null) {
            System.out.println("WARNING: Corpus file not found — morphology unavailable.");
            return;
        }
        System.out.println("Reading: " + corpusFile.getPath());

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(corpusFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty() || line.startsWith("LOCATION")) continue;
                String[] cols = line.split("\t");
                if (cols.length < 4) continue;

                // Location: (1:2:3:4) → ch:v:token as key, segment index
                String loc = cols[0].replaceAll("[()]", "");
                String[] parts = loc.split(":");
                if (parts.length < 4) continue;

                String key = parts[0] + ":" + parts[1] + ":" + parts[2];
                Map<String, String> seg = new LinkedHashMap<>();
                seg.put("segment", parts[3]);
                seg.put("form",    cols[1]);
                seg.put("tag",     cols[2]);
                seg.put("tagLabel", POS_LABELS.getOrDefault(cols[2], cols[2]));

                // Parse pipe-separated features
                for (String feat : cols[3].split("\\|")) {
                    if (feat.contains(":")) {
                        String[] kv = feat.split(":", 2);
                        seg.put(kv[0].toLowerCase(), kv[1]);
                    } else if (feat.equals("PREFIX") || feat.equals("STEM") || feat.equals("SUFFIX")) {
                        seg.put("type", feat);
                    } else if (!feat.isEmpty()) {
                        seg.put("attr_" + feat, "true");
                    }
                }

                MORPH.computeIfAbsent(key, k -> new ArrayList<>()).add(seg);
            }
        }
    }

    // ── API handlers ──────────────────────────────────────────────────────────

    // GET /api/chapters
    private static void handleChapters(HttpExchange ex) throws IOException {
        if (!isGet(ex)) return;
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Chapter chapter : Document.getChapters()) {
            if (!first) sb.append(",");
            first = false;
            int n = chapter.getChapterNumber();
            sb.append("{")
              .append("\"number\":").append(n).append(",")
              .append("\"nameArabic\":\"").append(escape(chapter.getName().toUnicode())).append("\",")
              .append("\"nameEnglish\":\"").append(escape(CHAPTER_NAMES[n - 1])).append("\",")
              .append("\"verseCount\":").append(chapter.getVerseCount())
              .append("}");
        }
        sb.append("]");
        sendJson(ex, sb.toString());
    }

    // GET /api/chapter/{n}
    private static void handleChapter(HttpExchange ex) throws IOException {
        if (!isGet(ex)) return;
        String path = ex.getRequestURI().getPath();
        try {
            int n = Integer.parseInt(path.substring("/api/chapter/".length()));
            Chapter chapter = Document.getChapter(n);
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"number\":").append(n).append(",")
              .append("\"nameArabic\":\"").append(escape(chapter.getName().toUnicode())).append("\",")
              .append("\"nameEnglish\":\"").append(escape(CHAPTER_NAMES[n - 1])).append("\",")
              .append("\"verseCount\":").append(chapter.getVerseCount()).append(",");
            if (chapter.getBismillah() != null) {
                sb.append("\"bismillah\":\"").append(escape(chapter.getBismillah().toUnicode())).append("\",");
            }
            sb.append("\"verses\":[");
            boolean first = true;
            for (Verse verse : chapter) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{")
                  .append("\"number\":").append(verse.getVerseNumber()).append(",")
                  .append("\"text\":\"").append(escape(verse.toUnicode())).append("\"")
                  .append("}");
            }
            sb.append("]}");
            sendJson(ex, sb.toString());
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Invalid chapter number");
        } catch (Exception e) {
            sendError(ex, 404, "Chapter not found");
        }
    }

    // GET /api/verse/{ch}/{v}
    private static void handleVerse(HttpExchange ex) throws IOException {
        if (!isGet(ex)) return;
        String path = ex.getRequestURI().getPath();
        try {
            String[] parts = path.substring("/api/verse/".length()).split("/");
            int ch = Integer.parseInt(parts[0]);
            int v  = Integer.parseInt(parts[1]);
            Verse verse = Document.getVerse(ch, v);
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"chapter\":").append(ch).append(",")
              .append("\"verse\":").append(v).append(",")
              .append("\"chapterName\":\"").append(escape(CHAPTER_NAMES[ch - 1])).append("\",")
              .append("\"text\":\"").append(escape(verse.toUnicode())).append("\",")
              .append("\"buckwalter\":\"").append(escape(verse.toBuckwalter())).append("\",")
              .append("\"tokens\":[");
            boolean first = true;
            for (Token token : verse.getTokens()) {
                if (!first) sb.append(",");
                first = false;
                int tn = token.getTokenNumber();
                // Get primary POS from morph data (stem segment)
                String posLabel = getPosLabel(ch, v, tn);
                sb.append("{")
                  .append("\"number\":").append(tn).append(",")
                  .append("\"text\":\"").append(escape(token.toUnicode())).append("\",")
                  .append("\"buckwalter\":\"").append(escape(token.toBuckwalter())).append("\",")
                  .append("\"pos\":\"").append(escape(posLabel)).append("\"")
                  .append("}");
            }
            sb.append("]}");
            sendJson(ex, sb.toString());
        } catch (Exception e) {
            sendError(ex, 404, "Verse not found");
        }
    }

    // GET /api/morph/{ch}/{v}/{t}
    private static void handleMorph(HttpExchange ex) throws IOException {
        if (!isGet(ex)) return;
        String path = ex.getRequestURI().getPath();
        try {
            String[] parts = path.substring("/api/morph/".length()).split("/");
            int ch = Integer.parseInt(parts[0]);
            int v  = Integer.parseInt(parts[1]);
            int t  = Integer.parseInt(parts[2]);
            String key = ch + ":" + v + ":" + t;
            List<Map<String, String>> segs = MORPH.getOrDefault(key, Collections.emptyList());

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"chapter\":").append(ch).append(",")
              .append("\"verse\":").append(v).append(",")
              .append("\"token\":").append(t).append(",")
              .append("\"segments\":[");
            boolean first = true;
            for (Map<String, String> seg : segs) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{");
                boolean ff = true;
                for (Map.Entry<String, String> e : seg.entrySet()) {
                    if (!ff) sb.append(",");
                    ff = false;
                    sb.append("\"").append(escape(e.getKey())).append("\":\"")
                      .append(escape(e.getValue())).append("\"");
                }
                sb.append("}");
            }
            sb.append("]}");
            sendJson(ex, sb.toString());
        } catch (Exception e) {
            sendError(ex, 400, "Invalid location");
        }
    }

    // GET /api/search?q=...
    private static void handleSearch(HttpExchange ex) throws IOException {
        if (!isGet(ex)) return;
        String query = ex.getRequestURI().getQuery();
        String q = "";
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("q=")) {
                    q = URLDecoder.decode(param.substring(2), "UTF-8");
                    break;
                }
            }
        }
        if (q.isEmpty()) { sendJson(ex, "[]"); return; }
        String qNorm = normalize(q);
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        int count = 0;
        for (Verse verse : Document.getVerses()) {
            if (normalize(verse.toUnicode()).contains(qNorm)) {
                if (!first) sb.append(",");
                first = false;
                int ch = verse.getChapterNumber();
                int v  = verse.getVerseNumber();
                sb.append("{")
                  .append("\"chapter\":").append(ch).append(",")
                  .append("\"verse\":").append(v).append(",")
                  .append("\"chapterName\":\"").append(escape(CHAPTER_NAMES[ch - 1])).append("\",")
                  .append("\"text\":\"").append(escape(verse.toUnicode())).append("\"")
                  .append("}");
                if (++count >= 100) break;
            }
        }
        sb.append("]");
        sendJson(ex, sb.toString());
    }

    // Static file server
    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        File file = new File(WEB_DIR + path);
        if (file.exists() && file.isFile()) {
            byte[] data = Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().set("Content-Type", contentType(path));
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.getResponseBody().close();
        } else {
            sendError(ex, 404, "Not found");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String getPosLabel(int ch, int v, int t) {
        List<Map<String, String>> segs = MORPH.get(ch + ":" + v + ":" + t);
        if (segs == null || segs.isEmpty()) return "";
        for (Map<String, String> seg : segs) {
            if ("STEM".equals(seg.get("type"))) {
                return seg.getOrDefault("tagLabel", seg.getOrDefault("tag", ""));
            }
        }
        return segs.get(0).getOrDefault("tagLabel", "");
    }

    private static String normalize(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if ((c >= '\u064B' && c <= '\u065F') || c == '\u0670' ||
                (c >= '\u06D6' && c <= '\u06DC') ||
                (c >= '\u06DF' && c <= '\u06E4') ||
                (c >= '\u06E7' && c <= '\u06E8') ||
                (c >= '\u06EA' && c <= '\u06ED') || c == '\u0640') continue;
            if (c == '\u0622' || c == '\u0623' || c == '\u0625' || c == '\u0671')
                sb.append('\u0627');
            else sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isGet(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) {
            sendError(ex, 405, "Method not allowed"); return false;
        }
        return true;
    }

    private static void sendJson(HttpExchange ex, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.getResponseBody().close();
    }

    private static void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] data = ("{\"error\":\"" + escape(msg) + "\"}").getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        ex.getResponseBody().write(data);
        ex.getResponseBody().close();
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css";
        if (path.endsWith(".js"))   return "application/javascript";
        return "text/plain";
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
