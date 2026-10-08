using OneLineCapture.Core;

namespace OneLineCapture;

// 단축키로 뜨는 한 줄 입력창. 엔터면 저장하고 사라지고, Esc나 다른 창을 누르면 그냥 사라진다 (spec.md §9)
internal sealed class CaptureForm : Form
{
    private readonly TextBox input = new();
    private readonly Label status = new();
    private readonly Func<string, Task<CaptureOutcome>> save;
    private bool saving;

    public CaptureForm(Func<string, Task<CaptureOutcome>> save)
    {
        this.save = save;
        Text = "한 줄 캡처";
        FormBorderStyle = FormBorderStyle.None;
        ShowInTaskbar = false;
        TopMost = true;
        StartPosition = FormStartPosition.Manual;
        BackColor = Color.White;
        Padding = new Padding(14, 12, 14, 10);
        ClientSize = new Size(560, 86);
        KeyPreview = true;

        input.Dock = DockStyle.Top;
        input.Font = new Font("Malgun Gothic", 15f);
        input.BorderStyle = BorderStyle.FixedSingle;
        input.PlaceholderText = "점심 9000원";
        input.MaxLength = 500;

        status.Dock = DockStyle.Bottom;
        status.Height = 24;
        status.Font = new Font("Malgun Gothic", 9.5f);
        status.ForeColor = Color.FromArgb(0x6E, 0x77, 0x81);
        status.Text = "엔터: 저장   Esc: 닫기";

        Controls.Add(input);
        Controls.Add(status);

        KeyDown += OnKeyDown;
        // 다른 곳을 누르면 닫는다. 저장 중에 닫으면 결과를 못 보므로 기다린다
        Deactivate += (_, _) => { if (!saving) Hide(); };
        Paint += (_, e) => ControlPaint.DrawBorder(e.Graphics, ClientRectangle, Color.FromArgb(0x4A, 0x7D, 0xFF), ButtonBorderStyle.Solid);
    }

    // 마우스가 있는 모니터의 위쪽 1/4에 띄운다. 듀얼 모니터에서 엉뚱한 화면에 뜨지 않게
    public void Popup()
    {
        var screen = Screen.FromPoint(Cursor.Position).WorkingArea;
        Location = new Point(screen.Left + (screen.Width - Width) / 2, screen.Top + screen.Height / 4);
        if (!saving)
        {
            status.ForeColor = Color.FromArgb(0x6E, 0x77, 0x81);
            status.Text = "엔터: 저장   Esc: 닫기";
        }
        Show();
        Activate();
        input.Focus();
        input.SelectAll();
    }

    private async void OnKeyDown(object? sender, KeyEventArgs e)
    {
        if (e.KeyCode == Keys.Escape)
        {
            e.SuppressKeyPress = true;
            Hide();
            return;
        }
        if (e.KeyCode != Keys.Enter || saving) return;
        e.SuppressKeyPress = true;

        var text = input.Text.Trim();
        if (text.Length == 0) return;

        saving = true;
        input.ReadOnly = true;
        status.ForeColor = Color.FromArgb(0x6E, 0x77, 0x81);
        status.Text = "저장 중...";
        try
        {
            var outcome = await save(text);
            if (outcome.Saved)
            {
                input.Clear();
                Hide();
            }
            else
            {
                // 실패하면 친 글자를 지우지 않는다. 오프라인 쓰기 큐는 없다 (spec.md §6)
                status.ForeColor = Color.FromArgb(0xCF, 0x22, 0x2E);
                status.Text = outcome.Message;
            }
        }
        finally
        {
            saving = false;
            input.ReadOnly = false;
        }
    }
}

internal sealed record CaptureOutcome(bool Saved, string Message);
