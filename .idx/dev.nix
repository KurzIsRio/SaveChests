{ pkgs, ... }: {
  # Nix channel to use.
  channel = "stable-24.11";

  # Packages to install.
  packages = [
    pkgs.jdk21  # Java Development Kit 21 for Minecraft 1.21+
    pkgs.maven  # Maven for building the project
  ];

  # VS Code extensions to install.
  idx = {
    extensions = [
      "vscjava.vscode-java-pack"  # Popular extension pack for Java development
    ];

    # Workspace lifecycle hooks.
    workspace = {
      # Runs when a workspace is first created.
      onCreate = {
        build = "mvn clean install";
      };
      # Runs every time the workspace is (re)started.
      onStart = {
        build = "mvn clean install";
      };
    };
  };
}
