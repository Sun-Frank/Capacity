Option Explicit

Dim shell, fso, scriptDir, psScript, cmd
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
psScript = scriptDir & "\local-start-all-silent.ps1"

cmd = "powershell -NoProfile -ExecutionPolicy Bypass -File """ & psScript & """ -FrontendPort 3000"

' WindowStyle = 0 => hidden, waitOnReturn = False => async
shell.Run cmd, 0, False
