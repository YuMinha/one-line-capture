using System.Net.Http.Json;
using System.Text.Json;

namespace OneLineCapture.Core;

public sealed class ApiException(int status, string? code, string message) : Exception(message)
{
    public int Status { get; } = status;
    public string? Code { get; } = code;

    // 필터가 보내는 UNAUTHORIZED일 때만 토큰이 죽은 것이다. 연결 코드가 틀린 400과 섞으면 안 된다
    public bool TokenRejected => Status == 401 && Code == "UNAUTHORIZED";
}

// 입력 전용 앱이라 부르는 API가 셋뿐이다 (stack.md §8). 브라우저가 아니라 CORS와 무관하다
public sealed class CaptureApi(HttpClient http)
{
    // 서버 주소는 여기 한 곳에만 둔다
    public const string BaseUrl = "https://one-line.duckdns.org";

    public static CaptureApi Create() => new(new HttpClient { BaseAddress = new Uri(BaseUrl), Timeout = TimeSpan.FromSeconds(10) });

    public async Task<string> LinkAsync(string code)
    {
        var body = await SendAsync(HttpMethod.Post, "/api/v1/auth/link", null, new { code });
        return body!.Value.GetProperty("token").GetString()!;
    }

    public async Task<JsonElement> CreateAsync(string token, string text) =>
        (await SendAsync(HttpMethod.Post, "/api/v1/captures", token, new { text }))!.Value;

    public Task LogoutAsync(string token) => SendAsync(HttpMethod.Post, "/api/v1/auth/logout", token, null);

    private async Task<JsonElement?> SendAsync(HttpMethod method, string path, string? token, object? payload)
    {
        using var request = new HttpRequestMessage(method, path);
        if (token != null) request.Headers.Add("X-API-Token", token);
        if (payload != null) request.Content = JsonContent.Create(payload);

        HttpResponseMessage response;
        try
        {
            response = await http.SendAsync(request);
        }
        catch (Exception e) when (e is HttpRequestException or TaskCanceledException)
        {
            throw new ApiException(0, null, CaptureText.ErrorMessage(0, null));
        }

        using (response)
        {
            var text = await response.Content.ReadAsStringAsync();
            var status = (int)response.StatusCode;
            if (!response.IsSuccessStatusCode)
            {
                throw new ApiException(status, CaptureText.ErrorField(text, "code"), CaptureText.ErrorMessage(status, text));
            }
            if (string.IsNullOrWhiteSpace(text)) return null;
            using var doc = JsonDocument.Parse(text);
            return doc.RootElement.Clone();
        }
    }
}
