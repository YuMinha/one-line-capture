using System.Text.Json;
using OneLineCapture.Core;
using Xunit;

namespace OneLineCapture.Tests;

public class CaptureTextTests
{
    private static JsonElement Json(string raw) => JsonDocument.Parse(raw).RootElement;

    [Fact]
    public void 지출은_금액에_콤마와_원을_붙이고_항목을_뒤에_둔다() =>
        Assert.Equal("지출로 저장됨 · 9,000원 점심",
            CaptureText.Saved(Json("""{"type":"EXPENSE","expense":{"amount":9000,"merchant":"점심"}}""")));

    [Fact]
    public void 소수점_금액도_정수로_보여준다() =>
        Assert.Equal("지출로 저장됨 · 15,000원",
            CaptureText.Saved(Json("""{"type":"EXPENSE","expense":{"amount":15000.00,"merchant":null}}""")));

    [Fact]
    public void 할일_마감은_한국_시각으로_보여준다() =>
        // 2026-10-07T06:00Z = KST 10/7 15:00
        Assert.Equal("할일로 저장됨 · 과제 제출 (10/7 15:00)",
            CaptureText.Saved(Json("""{"type":"TODO","todo":{"title":"과제 제출","dueAt":"2026-10-07T06:00:00Z","done":false}}""")));

    [Fact]
    public void 마감_없는_할일은_제목만() =>
        Assert.Equal("할일로 저장됨 · 우산 챙기기",
            CaptureText.Saved(Json("""{"type":"TODO","todo":{"title":"우산 챙기기","dueAt":null}}""")));

    [Fact]
    public void 링크는_메모를_앞세우고_없으면_URL()
    {
        Assert.Equal("링크로 저장됨 · 정리글", CaptureText.Saved(Json("""{"type":"LINK","link":{"url":"https://a.com","note":"정리글"}}""")));
        Assert.Equal("링크로 저장됨 · https://a.com", CaptureText.Saved(Json("""{"type":"LINK","link":{"url":"https://a.com","note":null}}""")));
    }

    [Fact]
    public void 서버_에러_메시지를_그대로_쓴다() =>
        Assert.Equal("코드가 틀렸거나 만료됐습니다",
            CaptureText.ErrorMessage(400, """{"error":{"code":"LINK_CODE_INVALID","message":"코드가 틀렸거나 만료됐습니다"}}"""));

    [Fact]
    public void 본문이_없으면_상태_코드로_문구를_고른다()
    {
        Assert.Equal("서버에 연결할 수 없습니다. 인터넷을 확인해 주세요", CaptureText.ErrorMessage(0, null));
        Assert.Equal("서버에 문제가 생겼습니다. 잠시 뒤에 다시 해 주세요", CaptureText.ErrorMessage(502, "<html>"));
    }

    [Fact]
    public void 필터의_UNAUTHORIZED만_토큰이_죽은_것으로_본다()
    {
        Assert.True(new ApiException(401, "UNAUTHORIZED", "").TokenRejected);
        Assert.False(new ApiException(401, "LOGIN_FAILED", "").TokenRejected);
        Assert.False(new ApiException(400, "LINK_CODE_INVALID", "").TokenRejected);
    }
}
