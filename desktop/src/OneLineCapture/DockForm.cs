using System.Runtime.InteropServices;

namespace OneLineCapture;

// 작업 표시줄 바로 위에 늘 떠 있는 한 줄 메모칸 (spec.md §9).
// 윈도우 11은 작업 표시줄 안에 다른 프로그램의 칸을 넣을 수 없어서(디스크밴드 폐지) 그 바로 위에 붙인다
internal sealed class DockForm : Form
{
    private static readonly Color Accent = Color.FromArgb(0x4A, 0x7D, 0xFF);
    private static readonly Color Error = Color.FromArgb(0xCF, 0x22, 0x2E);
    private const string IdleHint = "한 줄 던지기…";

    private readonly TextBox input = new();
    private readonly Panel grip = new();
    private readonly Panel holder = new();
    private Color dots = Color.FromArgb(0xA0, 0xA8, 0xB4);
    private readonly System.Windows.Forms.Timer hintTimer = new() { Interval = 5000 };
    private readonly DockSettings settings;
    private readonly Func<string, Task<CaptureOutcome>> save;
    private readonly Action requestLink;
    private Color border = Accent;
    private readonly System.Windows.Forms.Timer fullscreenTimer = new() { Interval = 1500 };
    private bool saving;
    private bool linked;
    private bool hiddenForFullscreen;

    public DockForm(DockSettings settings, Func<string, Task<CaptureOutcome>> save, Action requestLink, ContextMenuStrip menu)
    {
        this.settings = settings;
        this.save = save;
        this.requestLink = requestLink;

        Text = "한 줄 캡처";
        FormBorderStyle = FormBorderStyle.None;
        ShowInTaskbar = false;
        TopMost = true;
        StartPosition = FormStartPosition.Manual;
        // 96dpi 기준으로 잡은 크기다. 150% 화면에서는 1.5배로 커진다
        AutoScaleDimensions = new SizeF(96F, 96F);
        AutoScaleMode = AutoScaleMode.Dpi;
        ClientSize = new Size(320, 38);
        Padding = new Padding(2);
        ContextMenuStrip = menu;

        grip.Dock = DockStyle.Left;
        grip.Width = 14;
        grip.Cursor = Cursors.SizeAll;
        grip.Paint += (_, e) =>
        {
            using var brush = new SolidBrush(dots);
            for (var i = 0; i < 3; i++) e.Graphics.FillEllipse(brush, 5, 10 + i * 6, 3, 3);
        };
        grip.MouseDown += (_, e) => { if (e.Button == MouseButtons.Left) BeginDrag(); };

        // TextBox는 세로 가운데 정렬이 안 된다. 패널에 넣고 위 여백으로 맞춘다
        holder.Dock = DockStyle.Fill;
        holder.Padding = new Padding(8, 8, 8, 0);
        input.Dock = DockStyle.Fill;
        input.BorderStyle = BorderStyle.None;
        input.Font = new Font("Malgun Gothic", 11f);
        input.MaxLength = 500;
        input.PlaceholderText = IdleHint;
        input.KeyDown += OnKeyDown;
        input.MouseDown += (_, _) => { if (!linked) requestLink(); };
        holder.Controls.Add(input);

        Controls.Add(holder);
        Controls.Add(grip);

        hintTimer.Tick += (_, _) => { hintTimer.Stop(); ResetHint(); };
        fullscreenTimer.Tick += (_, _) => FollowFullscreen();
        fullscreenTimer.Start();
        Paint += (_, e) =>
        {
            using var pen = new Pen(border, 2);
            e.Graphics.DrawRectangle(pen, 1, 1, ClientSize.Width - 2, ClientSize.Height - 2);
        };
        Microsoft.Win32.SystemEvents.DisplaySettingsChanged += (_, _) => PlaceOnScreen();
        // 윈도우 설정에서 밝게/어둡게를 바꾸면 바로 따라간다
        Microsoft.Win32.SystemEvents.UserPreferenceChanged += (_, _) => ApplyTheme();
        ApplyTheme();
        Load += (_, _) => PlaceOnScreen();
    }

    // Alt+Tab 목록에 안 뜨게 한다. 작업 표시줄 소품이지 작업 창이 아니다
    protected override CreateParams CreateParams
    {
        get
        {
            var cp = base.CreateParams;
            cp.ExStyle |= 0x80; // WS_EX_TOOLWINDOW
            return cp;
        }
    }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
        // 윈도우 11의 둥근 모서리. 10에서는 이 호출이 실패하고 각진 채로 남는다
        var round = 2;
        _ = DwmSetWindowAttribute(Handle, 33, ref round, sizeof(int));
    }

    // 늘 맨 위에 있으면 전체 화면 영상·게임·발표 위에도 뜬다. 그동안만 숨었다가 끝나면 돌아온다
    private void FollowFullscreen()
    {
        var busy = SHQueryUserNotificationState(out var state) == 0 && state is 2 or 3 or 4;
        if (busy && Visible && !ContainsFocus)
        {
            hiddenForFullscreen = true;
            Hide();
        }
        else if (!busy && hiddenForFullscreen)
        {
            hiddenForFullscreen = false;
            if (settings.Visible) Show();
        }
    }

    // 보이기만 하고 포커스는 뺏지 않는다. 글을 쓰던 창에서 커서가 튀어나오면 안 된다.
    // 단축키로 부를 때는 FocusInput이 직접 Activate한다
    protected override bool ShowWithoutActivation => true;

    public void SetTheme(string theme)
    {
        settings.Theme = theme;
        settings.Save();
        ApplyTheme();
    }

    // 작업 표시줄 바로 위에 붙는 칸이라 작업 표시줄과 같은 색이어야 떠 보이지 않는다
    private void ApplyTheme()
    {
        var dark = settings.Theme switch
        {
            "dark" => true,
            "light" => false,
            _ => TaskbarIsDark(),
        };
        var back = dark ? Color.FromArgb(0x20, 0x20, 0x20) : Color.White;
        BackColor = back;
        holder.BackColor = back;
        input.BackColor = back;
        input.ForeColor = dark ? Color.FromArgb(0xF0, 0xF0, 0xF0) : Color.FromArgb(0x1F, 0x23, 0x28);
        grip.BackColor = dark ? Color.FromArgb(0x2C, 0x2C, 0x2C) : Color.FromArgb(0xF3, 0xF5, 0xF9);
        dots = dark ? Color.FromArgb(0x80, 0x80, 0x80) : Color.FromArgb(0xA0, 0xA8, 0xB4);
        grip.Invalidate();
        Invalidate();
    }

    private static bool TaskbarIsDark()
    {
        using var key = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(@"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
        // 값이 없으면(윈도우 10 초기 버전) 기본이 어두운 작업 표시줄이다
        return key?.GetValue("SystemUsesLightTheme") is not int light || light == 0;
    }

    public void SetLinked(bool value)
    {
        linked = value;
        input.ReadOnly = !value;
        ResetHint();
    }

    // 단축키를 누르면 어느 창을 보고 있든 이 칸으로 커서를 옮긴다
    public void FocusInput()
    {
        if (!Visible) Show();
        Activate();
        input.Focus();
        input.SelectAll();
    }

    private async void OnKeyDown(object? sender, KeyEventArgs e)
    {
        if (e.KeyCode == Keys.Escape)
        {
            e.SuppressKeyPress = true;
            input.Clear();
            return;
        }
        if (e.KeyCode != Keys.Enter || saving || !linked) return;
        e.SuppressKeyPress = true;

        var text = input.Text.Trim();
        if (text.Length == 0) return;

        saving = true;
        input.ReadOnly = true;
        try
        {
            var outcome = await save(text);
            if (outcome.Saved)
            {
                input.Clear();
                ShowHint(outcome.Message, Accent);
            }
            else
            {
                // 실패하면 친 글자를 지우지 않는다. 오프라인 쓰기 큐는 없다 (spec.md §6)
                ShowHint(outcome.Message, Error);
            }
        }
        finally
        {
            saving = false;
            input.ReadOnly = !linked;
        }
    }

    // 결과를 입력칸 안 안내 문구로 잠깐 보여준다. 팝업을 띄우면 다음 줄을 치는 흐름이 끊긴다
    private void ShowHint(string message, Color color)
    {
        input.PlaceholderText = message;
        border = color;
        Invalidate();
        if (input.TextLength > 0) toolTip.Show(message, this, 0, -28, 4000);
        hintTimer.Stop();
        hintTimer.Start();
    }

    private readonly ToolTip toolTip = new();

    private void ResetHint()
    {
        input.PlaceholderText = linked ? IdleHint : "눌러서 계정 연결";
        border = Accent;
        Invalidate();
    }

    // 저장된 자리가 지금 화면 안에 있으면 거기, 아니면 기본 자리(오른쪽 아래, 작업 표시줄 바로 위)
    private void PlaceOnScreen()
    {
        if (settings.X is int x && settings.Y is int y &&
            Screen.AllScreens.Any(s => s.WorkingArea.Contains(new Rectangle(x, y, Width, Height))))
        {
            Location = new Point(x, y);
            return;
        }
        var area = Screen.PrimaryScreen!.WorkingArea;
        Location = new Point(area.Right - Width - 16, area.Bottom - Height - 8);
    }

    private void BeginDrag()
    {
        ReleaseCapture();
        SendMessage(Handle, 0xA1, (IntPtr)2, IntPtr.Zero); // WM_NCLBUTTONDOWN, HTCAPTION
        // 끌기는 위 호출이 끝날 때 끝난다. 그 자리를 저장한다
        settings.X = Left;
        settings.Y = Top;
        settings.Save();
    }

    [DllImport("user32.dll")]
    private static extern bool ReleaseCapture();

    [DllImport("user32.dll")]
    private static extern IntPtr SendMessage(IntPtr hWnd, int msg, IntPtr wParam, IntPtr lParam);

    // 2: 전체 화면 앱, 3: 전체 화면 D3D(게임), 4: 프레젠테이션 모드
    [DllImport("shell32.dll")]
    private static extern int SHQueryUserNotificationState(out int state);

    [DllImport("dwmapi.dll")]
    private static extern int DwmSetWindowAttribute(IntPtr hwnd, int attribute, ref int value, int size);
}

internal sealed record CaptureOutcome(bool Saved, string Message);
