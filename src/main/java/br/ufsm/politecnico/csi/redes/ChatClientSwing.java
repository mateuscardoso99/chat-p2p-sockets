package br.ufsm.politecnico.csi.redes;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.List;

public class ChatClientSwing extends JFrame {

    private Usuario meuUsuario;
    private JList<?> listaChat;
    private DefaultListModel<Usuario> dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();
    private DatagramSocket datagramSocket;
    private ServerSocket serverSocket;
    private List<String> enderecos;
    private List<String> enderecosHostLocal;

    private final Object object = new Object();

    public ChatClientSwing() throws UnknownHostException, SocketException, IOException {
        enderecosHostLocal = Utils.getEnderecosHostLocal();

        setLayout(new GridBagLayout());
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.ONLINE.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.ONLINE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());

                    JMenuItem item = new JMenuItem("Fechar");
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());

                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent event) {
                            PainelChatPVT painel = (PainelChatPVT) tabbedPane.getComponentAt(tab);
                            try {
                                painel.socket.getOutputStream().write("exit\r\n".getBytes());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuário: ");

        if(nomeUsuario == null){
            System.exit(0);
        }

        while (nomeUsuario.trim().equals("")){
            nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuário: ","Entrada",JOptionPane.WARNING_MESSAGE);

            if(nomeUsuario == null){
                System.exit(0);
            }
        }

        meuUsuario = new Usuario(nomeUsuario, StatusUsuario.ONLINE, InetAddress.getLocalHost(), System.currentTimeMillis());
        setTitle("Chat P2P - Redes de Computadores USUÁRIO: "+meuUsuario.getNome().toUpperCase());
        setVisible(true);

        enderecos = getEnderecos();

        datagramSocket = new DatagramSocket(8084);
        serverSocket = new ServerSocket(8085);

        EmitirPing emitirPings = new EmitirPing(
            datagramSocket, 
            enderecos,
            listaChat, 
            dfListModel, 
            enderecosHostLocal, 
            meuUsuario
        );

        RecebePing recebePings = new RecebePing(datagramSocket, dfListModel, listaChat);

        new Thread(emitirPings).start();//thread que emite ping a cada 5s por UDP
        new Thread(recebePings).start();//thread que fica esperando os ping enviados a cada 5s por outros usuários por UDP

        new Thread(new Runnable() {
            @Override
            public void run(){
                try {
                    processaSocket();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private JComponent criaLista() {
        dfListModel = new DefaultListModel<>();
        listaChat = new JList<>(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JList<?> list = (JList<?>) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    synchronized(object){
                        if (chatsAbertos.add(user)) {
                            try {
                                tabbedPane.add(user.toString(), new PainelChatPVT(user, new Socket(user.getEndereco(), 8085)));
                                System.out.println("-------------"+tabbedPane.getTabCount());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        });
        return listaChat;
    }

    public class PainelChatPVT extends JPanel {
        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;
        Socket socket;

        PainelChatPVT(Usuario usuario, Socket socket) throws SocketException {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            areaChat.setEditable(false);

            this.usuario = usuario;
            this.socket = socket;

            campoEntrada = new JTextField();
            campoEntrada.setText("Envie uma mensagem");
            campoEntrada.setForeground(Color.GRAY);
            
            //evento de foco no input, pra manipular o placeholder
            campoEntrada.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent arg0) {
                    campoEntrada.setText("");
                    campoEntrada.setForeground(Color.BLACK);
                }
    
                @Override
                public void focusLost(FocusEvent arg0) {
                    campoEntrada.setText("Envie uma mensagem");
                    campoEntrada.setForeground(Color.GRAY);
                }
            });

            //evento de ação no input
            campoEntrada.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JTextField) e.getSource()).setText("");
                    sendMessage(e.getActionCommand());
                    campoEntrada.setText("");
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        
            //thread que fica esperando receber msg do usúario do chat
            new Thread(new Runnable() {
                @Override
                public void run() {
                    recebeMensagem();
                }
            }).start();
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public void setUsuario(Usuario usuario) {
            this.usuario = usuario;
        }

        /**
         * envia a mensagem para o usuário destinatário
         * @param chatArea
         * @param message
         * @param usuario
         */
        private void sendMessage(String message){
            try{
                if (!message.isEmpty()) {
                    synchronized (this.areaChat) {
                        this.areaChat.append("Você: " + message + "\n");
                    }
                    if (message.startsWith("%")) {
                        String[] msgArr = message.split(" ");
                        message = message.replace(msgArr[0], "");
                    }
                    message += "\r\n";
                    
                    this.socket.getOutputStream().write(message.getBytes());
                    System.out.println("MSG ENVIADA: "+message+" DESTINO: "+this.usuario.getNome()+" IP: "+this.usuario.getEndereco().getHostAddress()+"\n");
                }
            }catch(IOException e){
                e.printStackTrace();
                JOptionPane.showMessageDialog(tabbedPane, "Erro ao enviar mensagem", "erro", JOptionPane.ERROR_MESSAGE);
            }
        }

        /**
         * método que fica esperando chegar mensagens do usuário do chat
         * cada msg recebida é adicionada no chat.
         * fica no loop esperando as msg até receber uma msg com código "exit" que é enviado quando o usuário fecha o chat
         * e também o socket com esse usuário é fechado
         */
        private void recebeMensagem(){
            System.out.println("Thread para receber msg de "+this.usuario.getNome()+" iniciada.");

            try(BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()))) {
                String inputLine = in.readLine();

                while (!"exit".equalsIgnoreCase(inputLine) && inputLine != null) {
                    System.out.println("MSG RECEBIDA: " + inputLine + " USUÁRIO: "+this.usuario.getNome() + "\n");
                    
                    synchronized(this.areaChat){
                        this.areaChat.append(this.usuario.nome + " > " + inputLine + "\n");
                    }
                    inputLine = in.readLine();
                    /*lê uma linha de texto por vez. fica bloqueado até encontrar um '\n' ou '\r', 
                    quando encontrar, a linha é lida e o loop recomeça, aguardando a próxima linha
                    Se o fluxo em buffer tiver terminado e não houver nenhuma linha a ser lida, retorna NULL
                    indicando que acabou a leitura do buffer
                    */
                }

                /*
                    outra forma:
                 
                    byte[] b = new byte[4096];
                    int n;
                    while((n = this.socket.getInputStream().read(b)) != -1){
                        String message = new String(b,StandardCharsets.UTF_8);
                        System.out.println("MSG RECEBIDA: " + message + " USUÁRIO: "+this.usuario.getNome() + "\n");
                        synchronized(this.areaChat){
                            this.areaChat.append(this.usuario.nome + " > " + message + "\n");
                        }
                    }
                 */
                synchronized(object){
                    tabbedPane.remove(this);
                    chatsAbertos.remove(usuario);
                }
                JOptionPane.showMessageDialog(ChatClientSwing.this, "Chat encerrado pelo usuário " + usuario.getNome(), "Conexão encerreda", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            } finally{
                try {
                    this.socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + ((usuario == null) ? 0 : usuario.hashCode());
            result = prime * result + ((socket == null) ? 0 : socket.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PainelChatPVT other = (PainelChatPVT) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            if (usuario == null) {
                if (other.usuario != null)
                    return false;
            } else if (!usuario.equals(other.usuario))
                return false;
            if (socket == null) {
                if (other.socket != null)
                    return false;
            } else if (!socket.equals(other.socket))
                return false;
            return true;
        }

        private ChatClientSwing getEnclosingInstance() {
            return ChatClientSwing.this;
        }
    }

    /** 
     * método que fica ouvindo sockets recebidos
     * quando recebe, processa o socket em uma thread dedicada
     * busca o usuário que está criando o socket pelo IP
     * cria uma nova janela com esse usuário e o socket recebido
     * @throws IOException
     */
    private void processaSocket() throws IOException {
        while(true){
            Socket socket = serverSocket.accept();

            new Thread(new Runnable() {
                @Override
                public void run(){
                    try {
                        Usuario usuario = chatsAbertos.stream().filter(ch -> ch.getEndereco().equals(socket.getInetAddress())).findFirst().orElse(null);
                        
                        //verifica se não existe um chat aberto com o usuário e se ele não está na JList de usuários
                        if(usuario == null){
                            usuario = Utils.buscarUsuarioListModel(socket.getInetAddress(), listaChat);
                            if(usuario == null){
                                usuario = new Usuario(socket.getInetAddress().getHostAddress(), StatusUsuario.ONLINE, socket.getInetAddress(), System.currentTimeMillis());
                                dfListModel.addElement(usuario);
                            }
                        }
                        
                        /*
                        * acesso sincronizado as variaveis chatsAbertos e tabbedPane
                        * pois pode ocorrer delas serem acessadas ao mesmo tempo
                        * quando por exemplo o usuário clicar na lista de usuários pra abrir um chat com o usuário x
                        * isso implica em adicionar um novo objeto de chat em chatsAbertos e tabbedPane
                        * e se ao mesmo tempo receber uma msg dele, a thread que processa as msg recebidas pode não ver
                        * as novas alterações em chatsAbertos e tabbedPane o que pode gerar erros
                        */
                        synchronized(object){
                            tabbedPane.add(usuario.toString(), new PainelChatPVT(usuario, socket));
                            chatsAbertos.add(usuario);
                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public static void main(String[] args) throws UnknownHostException, SocketException, IOException{
        new ChatClientSwing();
    }

    public enum StatusUsuario {
        ONLINE, NAO_PERTURBE, VOLTO_LOGO
    }

    public static class Usuario {
        private String nome;
        private StatusUsuario status;
        private InetAddress endereco;
        private Long ultimoPing;

        public Usuario(String nome, StatusUsuario status, InetAddress endereco, Long ultimoPing) {
            this.nome = nome;
            this.status = status;
            this.endereco = endereco;
            this.ultimoPing = ultimoPing;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public InetAddress getEndereco() {
            return endereco;
        }

        public void setEndereco(InetAddress endereco) {
            this.endereco = endereco;
        }

        public Long getUltimoPing() {
            return ultimoPing;
        }

        public void setUltimoPing(Long ping) {
            this.ultimoPing = ping;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Usuario usuario = (Usuario) o;

            return nome.equals(usuario.nome) && endereco.equals(usuario.endereco);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((nome == null) ? 0 : nome.hashCode());
            result = prime * result + ((endereco == null) ? 0 : endereco.hashCode());
            return result;
        }

        public String toString() {
            return this.getNome() + " - " + this.getEndereco().getHostAddress() + " - " + " (" + getStatus().toString() + ")";
        }
    }

    private static List<String> getEnderecos(){
        List<String> enderecos = new LinkedList<>();
        enderecos.add("localhost");
        return enderecos;
    }    
}
