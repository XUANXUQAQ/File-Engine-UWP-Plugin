package file.engine.uwp.info;

import lombok.Data;

import javax.swing.*;

@Data
public class UWPInfo {
    private String DisplayName;
    private String Architecture;
    private String FamilyName;
    private String FullName;
    private String Name;
    private String Publisher;
    private String PublisherId;
    private String ResourceId;
    private String Version;
    private String Author;
    private String ProductId;
    private ImageIcon icon;
}
