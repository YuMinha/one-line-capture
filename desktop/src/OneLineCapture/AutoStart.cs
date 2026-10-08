using Microsoft.Win32;

namespace OneLineCapture;

// 켜 두지 않으면 단축키가 동작하지 않는다. 그래서 윈도우 시작과 함께 뜨게 한다. 관리자 권한이 필요 없는 HKCU를 쓴다
internal static class AutoStart
{
    private const string RunKey = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string Name = "OneLineCapture";

    public static bool Enabled
    {
        get
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey);
            return key?.GetValue(Name) is string;
        }
        set
        {
            using var key = Registry.CurrentUser.CreateSubKey(RunKey);
            if (value) key.SetValue(Name, $"\"{Environment.ProcessPath}\"");
            else key.DeleteValue(Name, false);
        }
    }
}
