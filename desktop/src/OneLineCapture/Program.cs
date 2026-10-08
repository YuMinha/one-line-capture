namespace OneLineCapture;

internal static class Program
{
    [STAThread]
    private static void Main()
    {
        // 두 번 실행되면 단축키 등록이 겹쳐 둘 다 실패한다. 하나만 살린다
        using var single = new Mutex(true, "OneLineCapture.SingleInstance", out var first);
        if (!first)
        {
            MessageBox.Show("한 줄 캡처가 이미 실행 중입니다. 작업 표시줄 오른쪽 트레이 아이콘을 확인하세요.",
                "한 줄 캡처", MessageBoxButtons.OK, MessageBoxIcon.Information);
            return;
        }

        ApplicationConfiguration.Initialize();
        Application.Run(new TrayContext());
    }
}
