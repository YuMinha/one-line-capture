using System.Runtime.InteropServices;

namespace OneLineCapture;

// 어느 프로그램을 보고 있든 단축키를 받으려면 윈도우에 직접 등록해야 한다. WinForms에는 이 기능이 없다
internal sealed class HotKey : NativeWindow, IDisposable
{
    private const int WmHotkey = 0x0312;
    private const uint ModAlt = 0x1, ModControl = 0x2, ModShift = 0x4, ModNoRepeat = 0x4000;
    private const uint VkSpace = 0x20;
    private const int Id = 1;

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool RegisterHotKey(IntPtr hWnd, int id, uint modifiers, uint vk);

    [DllImport("user32.dll")]
    private static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    public event Action? Pressed;

    // 실제로 잡힌 조합. 첫 후보를 다른 프로그램이 쓰고 있으면 다음 후보로 간다
    public string? Combination { get; private set; }

    public HotKey()
    {
        CreateHandle(new CreateParams());
        var candidates = new (uint mods, string name)[]
        {
            (ModControl | ModShift, "Ctrl+Shift+Space"),
            (ModControl | ModAlt, "Ctrl+Alt+Space"),
        };
        foreach (var (mods, name) in candidates)
        {
            if (RegisterHotKey(Handle, Id, mods | ModNoRepeat, VkSpace))
            {
                Combination = name;
                break;
            }
        }
    }

    protected override void WndProc(ref Message m)
    {
        if (m.Msg == WmHotkey && (int)m.WParam == Id) Pressed?.Invoke();
        base.WndProc(ref m);
    }

    public void Dispose()
    {
        if (Combination != null) UnregisterHotKey(Handle, Id);
        DestroyHandle();
    }
}
