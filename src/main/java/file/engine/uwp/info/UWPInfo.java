package file.engine.uwp.info;

import lombok.*;

import javax.swing.*;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@RequiredArgsConstructor
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
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ImageIcon icon;
}
