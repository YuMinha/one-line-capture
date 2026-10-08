using System.Text.Json;

namespace OneLineCapture;

// 메모창 위치와 표시 여부. 끌어서 옮긴 자리를 다음 실행에도 기억한다
internal sealed class DockSettings
{
    private static readonly string FilePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "OneLineCapture", "dock.json");

    public int? X { get; set; }
    public int? Y { get; set; }
    public bool Visible { get; set; } = true;

    public static DockSettings Load()
    {
        try
        {
            return File.Exists(FilePath)
                ? JsonSerializer.Deserialize<DockSettings>(File.ReadAllText(FilePath)) ?? new DockSettings()
                : new DockSettings();
        }
        catch (JsonException)
        {
            // 깨진 설정 때문에 앱이 안 뜨면 안 된다. 기본 위치로 시작한다
            return new DockSettings();
        }
    }

    public void Save()
    {
        Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
        File.WriteAllText(FilePath, JsonSerializer.Serialize(this));
    }
}
