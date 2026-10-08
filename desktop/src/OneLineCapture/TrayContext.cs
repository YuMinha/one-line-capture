using System.Diagnostics;
using OneLineCapture.Core;

namespace OneLineCapture;

// 창 없이 트레이에만 산다. 단축키를 받으려면 프로그램이 늘 떠 있어야 하기 때문이다
internal sealed class TrayContext : ApplicationContext
{
    public static readonly Icon AppIcon = LoadIcon();

    private readonly CaptureApi api = CaptureApi.Create();
    private readonly NotifyIcon tray = new();
    private readonly HotKey hotKey = new();
    private readonly DockSettings settings = DockSettings.Load();
    private readonly DockForm dock;
    private readonly ToolStripMenuItem openItem = new();
    private readonly ToolStripMenuItem dockItem = new("메모칸 보이기") { CheckOnClick = true };
    private readonly ToolStripMenuItem themeItem = new("메모칸 색");
    private readonly ToolStripMenuItem linkItem = new("계정 연결...");
    private readonly ToolStripMenuItem signOutItem = new("이 PC 연결 해제");
    private readonly ToolStripMenuItem autoStartItem = new("윈도우 시작 시 실행") { CheckOnClick = true };
    private string? token = TokenStore.Load();

    public TrayContext()
    {
        var menu = new ContextMenuStrip();
        dock = new DockForm(settings, SaveAsync, ShowLink, menu) { Icon = AppIcon };

        openItem.Click += (_, _) => OpenCapture();
        linkItem.Click += (_, _) => ShowLink();
        signOutItem.Click += async (_, _) => await SignOutAsync();
        autoStartItem.Click += (_, _) => AutoStart.Enabled = autoStartItem.Checked;
        dockItem.Click += (_, _) => SetDockVisible(dockItem.Checked);
        var webItem = new ToolStripMenuItem("목록·요약 보기 (웹)");
        webItem.Click += (_, _) => Process.Start(new ProcessStartInfo($"{CaptureApi.BaseUrl}/#/list") { UseShellExecute = true });
        var exitItem = new ToolStripMenuItem("종료");
        exitItem.Click += (_, _) => ExitThread();

        foreach (var (value, label) in new[] { ("auto", "작업 표시줄 따라가기"), ("light", "밝게"), ("dark", "어둡게") })
        {
            var item = new ToolStripMenuItem(label) { Tag = value };
            item.Click += (_, _) => { dock.SetTheme(value); RefreshMenu(); };
            themeItem.DropDownItems.Add(item);
        }
        menu.Items.AddRange([openItem, webItem, dockItem, themeItem, new ToolStripSeparator(), linkItem, signOutItem, autoStartItem, new ToolStripSeparator(), exitItem]);
        menu.Opening += (_, _) => RefreshMenu();

        tray.Icon = AppIcon;
        tray.Text = "한 줄 캡처";
        tray.ContextMenuStrip = menu;
        tray.Visible = true;
        tray.MouseClick += (_, e) => { if (e.Button == MouseButtons.Left) OpenCapture(); };

        hotKey.Pressed += OpenCapture;
        dock.SetLinked(token != null);
        if (settings.Visible) dock.Show();
        RefreshMenu();

        if (hotKey.Combination == null)
        {
            Notify("단축키를 등록하지 못했습니다. 다른 프로그램이 쓰고 있습니다. 트레이 아이콘을 눌러 입력하세요.");
        }

        if (token == null) ShowLink();
        else Notify(ReadyMessage("한 줄 캡처가 켜졌습니다."));
    }

    private void RefreshMenu()
    {
        openItem.Text = hotKey.Combination == null ? "한 줄 입력" : $"한 줄 입력 ({hotKey.Combination})";
        openItem.Enabled = token != null;
        linkItem.Visible = token == null;
        signOutItem.Visible = token != null;
        autoStartItem.Checked = AutoStart.Enabled;
        dockItem.Checked = dock.Visible;
        foreach (ToolStripMenuItem item in themeItem.DropDownItems) item.Checked = (string?)item.Tag == settings.Theme;
    }

    private void SetDockVisible(bool visible)
    {
        settings.Visible = visible;
        settings.Save();
        if (visible) dock.Show(); else dock.Hide();
    }

    private void OpenCapture()
    {
        if (token == null) ShowLink();
        else dock.FocusInput();
    }

    private void ShowLink()
    {
        using var form = new LinkForm(api);
        if (form.ShowDialog() != DialogResult.OK || form.Token == null) return;
        token = form.Token;
        TokenStore.Save(token);
        // 처음 연결할 때 자동 실행을 켠다. 재부팅 뒤에 단축키가 안 먹으면 앱이 고장 난 것처럼 보인다
        AutoStart.Enabled = true;
        dock.SetLinked(true);
        RefreshMenu();
        Notify(ReadyMessage("연결했습니다."));
    }

    private async Task<CaptureOutcome> SaveAsync(string text)
    {
        if (token == null) return new CaptureOutcome(false, "먼저 계정을 연결해 주세요");
        try
        {
            // 결과는 메모칸 안에 보인다. 매번 알림까지 띄우면 시끄럽다
            var message = CaptureText.Saved(await api.CreateAsync(token, text));
            return new CaptureOutcome(true, message);
        }
        catch (ApiException e)
        {
            if (e.TokenRejected)
            {
                // 웹에서 이 PC를 로그아웃했거나 토큰이 사라졌다. 다시 연결하게 한다
                token = null;
                TokenStore.Clear();
                dock.SetLinked(false);
                RefreshMenu();
            }
            return new CaptureOutcome(false, e.Message);
        }
    }

    private async Task SignOutAsync()
    {
        var old = token;
        token = null;
        TokenStore.Clear();
        dock.SetLinked(false);
        RefreshMenu();
        // 서버에서 못 지워도 이 PC에서는 나간다
        if (old != null)
        {
            try { await api.LogoutAsync(old); } catch (ApiException) { }
        }
        Notify("이 PC의 연결을 해제했습니다.");
    }

    // 조합 이름 뒤에 조사를 붙이지 않는다. "Space으로"처럼 받침이 안 맞는다
    private string ReadyMessage(string head) =>
        hotKey.Combination == null
            ? $"{head} 작업 표시줄 위 메모칸에 바로 입력하세요."
            : $"{head} 메모칸에 바로 입력하거나, 어디서든 {hotKey.Combination} 키를 누르세요.";

    private void Notify(string message) => tray.ShowBalloonTip(3000, "한 줄 캡처", message, ToolTipIcon.None);

    protected override void ExitThreadCore()
    {
        tray.Visible = false;
        hotKey.Dispose();
        tray.Dispose();
        dock.Dispose();
        base.ExitThreadCore();
    }

    private static Icon LoadIcon()
    {
        using var stream = typeof(TrayContext).Assembly.GetManifestResourceStream("app.ico")!;
        return new Icon(stream);
    }
}
