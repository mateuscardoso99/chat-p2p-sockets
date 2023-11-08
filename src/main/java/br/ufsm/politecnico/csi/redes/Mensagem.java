package br.ufsm.politecnico.csi.redes;

import br.ufsm.politecnico.csi.redes.ChatClientSwing.StatusUsuario;

public class Mensagem {
    private TipoMensagem tipo;
    private String nomeUsuario;
    private StatusUsuario status;

    public enum TipoMensagem{
        PING
    }

    public TipoMensagem getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensagem tipo) {
        this.tipo = tipo;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public void setStatus(StatusUsuario status) {
        this.status = status;
    }    
}
