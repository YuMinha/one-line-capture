using System.Security.Cryptography;
using System.Text;

namespace OneLineCapture;

// 토큰은 DPAPI로 암호화해 %APPDATA%에 둔다. 같은 윈도우 계정으로 로그인한 사람만 풀 수 있다 (stack.md §8)
internal static class TokenStore
{
    private static readonly string Dir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "OneLineCapture");
    private static readonly string FilePath = Path.Combine(Dir, "token.dat");

    public static string? Load()
    {
        try
        {
            if (!File.Exists(FilePath)) return null;
            var plain = ProtectedData.Unprotect(File.ReadAllBytes(FilePath), null, DataProtectionScope.CurrentUser);
            return Encoding.UTF8.GetString(plain);
        }
        catch (CryptographicException)
        {
            // 다른 PC에서 복사해 온 파일이면 못 푼다. 연결 코드로 다시 붙게 한다
            return null;
        }
    }

    public static void Save(string token)
    {
        Directory.CreateDirectory(Dir);
        var cipher = ProtectedData.Protect(Encoding.UTF8.GetBytes(token), null, DataProtectionScope.CurrentUser);
        File.WriteAllBytes(FilePath, cipher);
    }

    public static void Clear()
    {
        if (File.Exists(FilePath)) File.Delete(FilePath);
    }
}
