using System.Diagnostics;
using OneLineCapture.Core;

namespace OneLineCapture;

// 아이디·비밀번호 화면은 두지 않는다. 웹에서 받은 8자리 코드로만 붙는다 (stack.md §8)
internal sealed class LinkForm : Form
{
    private readonly TextBox code = new();
    private readonly Button link = new();
    private readonly Label status = new();
    private readonly CaptureApi api;

    public string? Token { get; private set; }

    public LinkForm(CaptureApi api)
    {
        this.api = api;
        Text = "한 줄 캡처 — 계정 연결";
        Icon = TrayContext.AppIcon;
        FormBorderStyle = FormBorderStyle.FixedDialog;
        MaximizeBox = false;
        MinimizeBox = false;
        StartPosition = FormStartPosition.CenterScreen;
        ClientSize = new Size(440, 230);
        Font = new Font("Malgun Gothic", 10f);
        Padding = new Padding(16);

        var help = new Label
        {
            Dock = DockStyle.Top,
            Height = 64,
            Text = "웹(one-line.duckdns.org)의 계정 화면에서 \"코드 받기\"를 누르면 8자리 코드가 나옵니다.\n그 코드를 넣으면 웹과 같은 기록에 저장됩니다.",
        };
        var open = new LinkLabel { Dock = DockStyle.Top, Height = 28, Text = "웹에서 코드 받기" };
        open.LinkClicked += (_, _) => Process.Start(new ProcessStartInfo($"{CaptureApi.BaseUrl}/#/account") { UseShellExecute = true });

        code.Dock = DockStyle.Top;
        code.Font = new Font("Consolas", 16f);
        code.CharacterCasing = CharacterCasing.Upper;
        code.MaxLength = 12;
        code.PlaceholderText = "K7QM-3XPD";

        link.Dock = DockStyle.Top;
        link.Height = 36;
        link.Text = "연결";
        link.Click += async (_, _) => await LinkAsync();
        AcceptButton = link;

        status.Dock = DockStyle.Fill;
        status.ForeColor = Color.FromArgb(0xCF, 0x22, 0x2E);

        // Dock=Top은 나중에 넣은 것이 위로 간다. 화면 위→아래 순서의 반대로 넣는다
        Controls.Add(status);
        Controls.Add(link);
        Controls.Add(code);
        Controls.Add(open);
        Controls.Add(help);
    }

    private async Task LinkAsync()
    {
        var value = code.Text.Trim();
        if (value.Length == 0) return;
        link.Enabled = false;
        status.Text = "연결 중...";
        try
        {
            Token = await api.LinkAsync(value);
            DialogResult = DialogResult.OK;
            Close();
        }
        catch (ApiException e)
        {
            status.Text = e.Message;
        }
        finally
        {
            link.Enabled = true;
        }
    }
}
