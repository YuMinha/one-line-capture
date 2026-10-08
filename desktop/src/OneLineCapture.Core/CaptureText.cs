using System.Globalization;
using System.Text.Json;

namespace OneLineCapture.Core;

// 서버 응답을 사람이 읽을 한 줄로 바꾼다. 안드로이드 CaptureText.kt, 웹 format.js와 같은 문구를 쓴다
public static class CaptureText
{
    // 서버는 UTC로 준다. 사람은 한국 시각으로 읽는다 (stack.md §2.2)
    private static readonly TimeZoneInfo Kst = FindKst();

    public static string Label(string? type) => type switch
    {
        "EXPENSE" => "지출",
        "TODO" => "할일",
        "LINK" => "링크",
        _ => type ?? "",
    };

    // 세 라벨 모두 받침이 없거나 ㄹ 받침이라 '으로'가 아니라 '로'다
    public static string Saved(JsonElement capture)
    {
        var type = capture.TryGetProperty("type", out var t) ? t.GetString() : null;
        var head = $"{Label(type)}로 저장됨";
        var detail = Detail(capture, type);
        return string.IsNullOrWhiteSpace(detail) ? head : $"{head} · {detail}";
    }

    private static string? Detail(JsonElement capture, string? type)
    {
        switch (type)
        {
            case "EXPENSE" when capture.TryGetProperty("expense", out var e):
                var amount = e.TryGetProperty("amount", out var a) && a.ValueKind == JsonValueKind.Number
                    ? a.GetDecimal().ToString("#,0", CultureInfo.InvariantCulture) + "원"
                    : null;
                return string.Join(" ", new[] { amount, Str(e, "merchant") }.Where(s => s != null));
            case "TODO" when capture.TryGetProperty("todo", out var todo):
                var title = Str(todo, "title");
                var dueRaw = Str(todo, "dueAt");
                if (dueRaw == null) return title;
                var due = TimeZoneInfo.ConvertTime(DateTimeOffset.Parse(dueRaw, CultureInfo.InvariantCulture), Kst);
                return $"{title} ({due:M/d HH:mm})";
            case "LINK" when capture.TryGetProperty("link", out var link):
                return Str(link, "note") ?? Str(link, "url");
            default:
                return null;
        }
    }

    // 서버 에러는 { error: { code, message } } 한 모양이다. 그 메시지를 그대로 보여준다
    public static string ErrorMessage(int status, string? body)
    {
        var fromServer = ErrorField(body, "message");
        if (!string.IsNullOrWhiteSpace(fromServer)) return fromServer;
        return status switch
        {
            0 => "서버에 연결할 수 없습니다. 인터넷을 확인해 주세요",
            >= 500 => "서버에 문제가 생겼습니다. 잠시 뒤에 다시 해 주세요",
            _ => $"요청을 처리하지 못했습니다 ({status})",
        };
    }

    public static string? ErrorField(string? body, string field)
    {
        if (string.IsNullOrWhiteSpace(body)) return null;
        try
        {
            using var doc = JsonDocument.Parse(body);
            return doc.RootElement.TryGetProperty("error", out var error) && error.TryGetProperty(field, out var value)
                ? value.GetString()
                : null;
        }
        catch (JsonException)
        {
            return null;
        }
    }

    private static string? Str(JsonElement obj, string key) =>
        obj.TryGetProperty(key, out var v) && v.ValueKind == JsonValueKind.String && !string.IsNullOrWhiteSpace(v.GetString())
            ? v.GetString()
            : null;

    // 윈도우는 "Korea Standard Time", 리눅스(테스트)는 "Asia/Seoul"로 찾는다
    private static TimeZoneInfo FindKst()
    {
        try { return TimeZoneInfo.FindSystemTimeZoneById("Asia/Seoul"); }
        catch (TimeZoneNotFoundException) { return TimeZoneInfo.FindSystemTimeZoneById("Korea Standard Time"); }
    }
}
