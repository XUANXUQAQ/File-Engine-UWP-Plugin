$PkgMgr = [Windows.Management.Deployment.PackageManager,Windows.Web,ContentType=WindowsRuntime]::new()
$PkgMgr.FindPackages() | Select-Object DisplayName -ExpandProperty Id