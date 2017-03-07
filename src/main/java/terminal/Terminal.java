package terminal;

import java.io.Serializable;

/**
 * Created by $Hamid on 3/7/2017.
 */
public class Terminal implements Serializable {
    private Integer id;
    private String type;

    public Terminal(Integer id, String type) {
        this.id = id;
        this.type = type;
    }

    public Integer getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
