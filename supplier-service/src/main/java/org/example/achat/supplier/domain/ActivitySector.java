package org.example.achat.supplier.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_sectors")
public class ActivitySector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSecteurActivite;

    private String codeSecteurActivite;

    private String libelleSecteurActivite;

    public Long getIdSecteurActivite() {
        return idSecteurActivite;
    }

    public void setIdSecteurActivite(Long idSecteurActivite) {
        this.idSecteurActivite = idSecteurActivite;
    }

    public String getCodeSecteurActivite() {
        return codeSecteurActivite;
    }

    public void setCodeSecteurActivite(String codeSecteurActivite) {
        this.codeSecteurActivite = codeSecteurActivite;
    }

    public String getLibelleSecteurActivite() {
        return libelleSecteurActivite;
    }

    public void setLibelleSecteurActivite(String libelleSecteurActivite) {
        this.libelleSecteurActivite = libelleSecteurActivite;
    }
}
