import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** CPU Scheduling Simulator for Java 17. Lower priority number means higher priority. */
public class CPUSchedulingSimulator extends JFrame {
    private final DefaultTableModel input = new DefaultTableModel(new Object[]{"Process ID", "Arrival Time", "Burst Time", "Priority"}, 0);
    private final DefaultTableModel output = new DefaultTableModel(new Object[]{"Process", "Arrival", "Burst", "Priority", "First Start", "Completion", "Turnaround", "Waiting", "Response"}, 0);
    private final DefaultTableModel compare = new DefaultTableModel(new Object[]{"Algorithm", "Avg Waiting", "Avg Turnaround", "Avg Response", "CPU Utilization"}, 0);
    private final JTable inputTable = new JTable(input);
    private final JComboBox<String> algorithm = new JComboBox<>(new String[]{"FCFS", "SJF (Non-preemptive)", "Priority (Non-preemptive)", "Round Robin", "Compare All"});
    private final JSpinner quantum = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
    private final GanttPanel gantt = new GanttPanel();
    private final JLabel status = new JLabel("Ready. Edit the sample processes and click Run Simulation.");
    private final JTextArea info = new JTextArea();

    public CPUSchedulingSimulator() {
        super("CPU Scheduling Simulator - Java 17");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 760); setMinimumSize(new Dimension(900, 650)); setLocationRelativeTo(null);
        JLabel title = new JLabel("CPU Scheduling Simulator"); title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Operating Systems Mini Project | Java 17 + Swing");
        JPanel header = new JPanel(new GridLayout(2,1)); header.add(title); header.add(subtitle);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Algorithm:")); controls.add(algorithm); controls.add(new JLabel("RR quantum:")); controls.add(quantum);
        JButton run = new JButton("Run Simulation"); run.addActionListener(e -> runSimulation()); controls.add(run);
        JButton sample = new JButton("Load Sample"); sample.addActionListener(e -> loadSample()); controls.add(sample);
        JButton add = new JButton("Add Process"); add.addActionListener(e -> input.addRow(new Object[]{"P"+(input.getRowCount()+1),0,1,1})); controls.add(add);
        JButton remove = new JButton("Remove Selected"); remove.addActionListener(e -> { int r=inputTable.getSelectedRow(); if(r>=0) input.removeRow(r); else JOptionPane.showMessageDialog(this,"Select a row first."); }); controls.add(remove);
        JPanel north = new JPanel(new BorderLayout(5,5)); north.setBorder(BorderFactory.createEmptyBorder(10,10,5,10)); north.add(header,BorderLayout.NORTH); north.add(controls,BorderLayout.CENTER);
        inputTable.setRowHeight(24);
        JPanel inputPanel = new JPanel(new BorderLayout()); inputPanel.setBorder(BorderFactory.createTitledBorder("1. Process Input")); inputPanel.add(new JScrollPane(inputTable));
        JPanel ganttPanel = new JPanel(new BorderLayout()); ganttPanel.setBorder(BorderFactory.createTitledBorder("2. CPU Gantt Chart")); ganttPanel.add(new JScrollPane(gantt));
        JTable outputTable = new JTable(output); outputTable.setAutoCreateRowSorter(true);
        JTable compareTable = new JTable(compare);
        JTabbedPane tabs = new JTabbedPane(); tabs.addTab("Process Results", new JScrollPane(outputTable)); tabs.addTab("Algorithm Comparison", new JScrollPane(compareTable));
        info.setEditable(false); info.setLineWrap(true); info.setWrapStyleWord(true); info.setText(definitions()); tabs.addTab("Definitions", new JScrollPane(info));
        JSplitPane upper = new JSplitPane(JSplitPane.VERTICAL_SPLIT,inputPanel,ganttPanel); upper.setResizeWeight(.40);
        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT,upper,tabs); main.setResizeWeight(.55);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.setBorder(BorderFactory.createEmptyBorder(5,10,8,10)); bottom.add(status);
        add(north,BorderLayout.NORTH); add(main,BorderLayout.CENTER); add(bottom,BorderLayout.SOUTH);
        loadSample();
        algorithm.addActionListener(e -> quantum.setEnabled(algorithm.getSelectedItem().equals("Round Robin") || algorithm.getSelectedItem().equals("Compare All")));
        quantum.setEnabled(false);
    }

    private void loadSample() {
        input.setRowCount(0);
        input.addRow(new Object[]{"P1",0,5,2}); input.addRow(new Object[]{"P2",1,3,1});
        input.addRow(new Object[]{"P3",2,8,3}); input.addRow(new Object[]{"P4",3,6,2});
        status.setText("Sample processes loaded.");
    }

    private List<Proc> readInput() {
        if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();
        if (input.getRowCount()==0) throw new IllegalArgumentException("Add at least one process.");
        List<Proc> ps=new ArrayList<>(); Set<String> ids=new HashSet<>();
        for(int i=0;i<input.getRowCount();i++) {
            String id=Objects.toString(input.getValueAt(i,0),"").trim();
            if(id.isEmpty() || !ids.add(id)) throw new IllegalArgumentException("Process IDs must be non-empty and unique. Check row "+(i+1)+".");
            int a=integer(input.getValueAt(i,1),"Arrival time",i), b=integer(input.getValueAt(i,2),"Burst time",i), p=integer(input.getValueAt(i,3),"Priority",i);
            if(a<0 || b<=0 || p<0) throw new IllegalArgumentException("Arrival and priority must be >= 0; burst must be > 0 (row "+(i+1)+").");
            ps.add(new Proc(id,a,b,p));
        }
        return ps;
    }
    private int integer(Object v,String field,int row) {
        try { double d=Double.parseDouble(Objects.toString(v,"")); if(!Double.isFinite(d)||d!=Math.rint(d)||d<Integer.MIN_VALUE||d>Integer.MAX_VALUE) throw new Exception(); return (int)d; }
        catch(Exception e){ throw new IllegalArgumentException(field+" must be a whole number in row "+(row+1)+"."); }
    }
    private void runSimulation() {
        try {
            List<Proc> ps=readInput(); String selected=(String)algorithm.getSelectedItem(); int q=(Integer)quantum.getValue();
            output.setRowCount(0); compare.setRowCount(0);
            if(selected.equals("Compare All")) {
                List<Result> all=List.of(Algorithms.fcfs(ps),Algorithms.sjf(ps),Algorithms.priority(ps),Algorithms.rr(ps,q));
                for(Result r:all) compare.addRow(new Object[]{r.name,fmt(r.avgWait),fmt(r.avgTat),fmt(r.avgResp),fmt(r.util)+"%"});
                show(all.get(0)); gantt.setSegments(all.get(0).segments); info.setText("Comparison completed. The process table and Gantt chart show FCFS. The Algorithm Comparison tab contains average metrics for all four algorithms.");
                status.setText("Comparison complete. Gantt chart currently shows FCFS.");
            } else {
                Result r=switch(selected) { case "SJF (Non-preemptive)" -> Algorithms.sjf(ps); case "Priority (Non-preemptive)" -> Algorithms.priority(ps); case "Round Robin" -> Algorithms.rr(ps,q); default -> Algorithms.fcfs(ps); };
                show(r); gantt.setSegments(r.segments); info.setText(r.summary()); status.setText("Simulation completed: "+r.name);
            }
        } catch(Exception ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Input Error",JOptionPane.ERROR_MESSAGE); status.setText("Please correct the input and try again."); }
    }
    private void show(Result r) {
        output.setRowCount(0);
        for(Row x:r.rows) output.addRow(new Object[]{x.pid,x.arrival,x.burst,x.priority,x.first,x.completion,x.tat,x.wait,x.response});
        info.setText(r.summary());
    }
    private static String fmt(double n){return String.format(Locale.US,"%.2f",n);}
    private static String definitions(){return "CPU Scheduling Simulator - Definitions\n\nArrival Time: when a process enters the ready queue.\nBurst Time: CPU time required by a process.\nCompletion Time: when a process finishes.\nTurnaround Time = Completion Time - Arrival Time.\nWaiting Time = Turnaround Time - Burst Time.\nResponse Time = First Start Time - Arrival Time.\nCPU Utilization = Busy CPU Time / final completion time x 100.\n\nFCFS: executes in arrival order.\nSJF: selects the available process with shortest burst (non-preemptive).\nPriority: selects the available process with the smallest priority number (non-preemptive).\nRound Robin: each process receives up to a fixed time quantum (preemptive).\n\nContext-switch overhead is ignored. This program simulates scheduling; it does not change the computer's real OS scheduler.";}

    private static class Proc { final String id; final int arrival,burst,priority; Proc(String i,int a,int b,int p){id=i;arrival=a;burst=b;priority=p;} }
    private static class Seg { final String id; final int start,end; Seg(String i,int s,int e){id=i;start=s;end=e;} }
    private static class Row {
        final String pid; final int arrival,burst,priority,first,completion,tat,wait,response;
        Row(Proc p,int f,int c){pid=p.id;arrival=p.arrival;burst=p.burst;priority=p.priority;first=f;completion=c;tat=c-p.arrival;wait=tat-p.burst;response=f-p.arrival;}
    }
    private static class Result {
        final String name; final List<Seg> segments; final List<Row> rows; final double avgWait,avgTat,avgResp,util;
        Result(String n,List<Seg>s,List<Row>r){name=n;segments=s;rows=r;avgWait=r.stream().mapToInt(x->x.wait).average().orElse(0);avgTat=r.stream().mapToInt(x->x.tat).average().orElse(0);avgResp=r.stream().mapToInt(x->x.response).average().orElse(0);int finish=s.stream().mapToInt(x->x.end).max().orElse(0);int busy=s.stream().filter(x->!x.id.equals("IDLE")).mapToInt(x->x.end-x.start).sum();util=finish==0?0:100.0*busy/finish;}
        String summary(){return "Algorithm: "+name+"\nAverage waiting time: "+fmt(avgWait)+"\nAverage turnaround time: "+fmt(avgTat)+"\nAverage response time: "+fmt(avgResp)+"\nCPU utilization: "+fmt(util)+"%\n\nTAT = CT - AT\nWT = TAT - BT\nRT = First Start - AT";}
    }
    private static class Algorithms {
        static void add(List<Seg>s,String id,int a,int b){if(b<=a)return;if(!s.isEmpty()){Seg last=s.get(s.size()-1);if(last.id.equals(id)&&last.end==a){s.set(s.size()-1,new Seg(id,last.start,b));return;}}s.add(new Seg(id,a,b));}
        static Result finish(List<Proc>ps,List<Seg>s,Map<String,Integer>first,Map<String,Integer>done,String name){List<Row> rows=new ArrayList<>();for(Proc p:ps)rows.add(new Row(p,first.get(p.id),done.get(p.id)));rows.sort(Comparator.comparingInt((Row r)->r.arrival).thenComparing(r->r.pid));return new Result(name,s,rows);}
        static Result fcfs(List<Proc>ps){List<Proc>order=new ArrayList<>(ps);order.sort(Comparator.comparingInt((Proc p)->p.arrival).thenComparing(p->p.id));int t=0;List<Seg>s=new ArrayList<>();Map<String,Integer>f=new HashMap<>(),d=new HashMap<>();for(Proc p:order){if(t<p.arrival){add(s,"IDLE",t,p.arrival);t=p.arrival;}f.put(p.id,t);add(s,p.id,t,t+p.burst);t+=p.burst;d.put(p.id,t);}return finish(ps,s,f,d,"FCFS");}
        static Result sjf(List<Proc>ps){return nonpreemptive(ps,false);}
        static Result priority(List<Proc>ps){return nonpreemptive(ps,true);}
        static Result nonpreemptive(List<Proc>ps,boolean byPriority){Map<String,Proc>rem=new LinkedHashMap<>();for(Proc p:ps)rem.put(p.id,p);int t=0;List<Seg>s=new ArrayList<>();Map<String,Integer>f=new HashMap<>(),d=new HashMap<>();while(!rem.isEmpty()){List<Proc>a=new ArrayList<>();for(Proc p:rem.values())if(p.arrival<=t)a.add(p);if(a.isEmpty()){int next=rem.values().stream().mapToInt(p->p.arrival).min().orElse(t);add(s,"IDLE",t,next);t=next;for(Proc p:rem.values())if(p.arrival<=t)a.add(p);}Comparator<Proc>cmp=byPriority?Comparator.comparingInt((Proc p)->p.priority).thenComparingInt(p->p.arrival).thenComparing(p->p.id):Comparator.comparingInt((Proc p)->p.burst).thenComparingInt(p->p.arrival).thenComparing(p->p.id);Proc p=a.stream().min(cmp).orElseThrow();f.put(p.id,t);add(s,p.id,t,t+p.burst);t+=p.burst;d.put(p.id,t);rem.remove(p.id);}return finish(ps,s,f,d,byPriority?"Priority (Non-preemptive)":"SJF (Non-preemptive)");}
        static Result rr(List<Proc>ps,int q){if(q<=0)throw new IllegalArgumentException("Quantum must be greater than zero.");List<Proc>order=new ArrayList<>(ps);order.sort(Comparator.comparingInt((Proc p)->p.arrival).thenComparing(p->p.id));Map<String,Integer>rem=new HashMap<>(),f=new HashMap<>(),d=new HashMap<>();for(Proc p:ps)rem.put(p.id,p.burst);Queue<Proc>ready=new ArrayDeque<>();List<Seg>s=new ArrayList<>();int t=0,i=0;while(d.size()<ps.size()){if(ready.isEmpty()&&i<order.size()&&t<order.get(i).arrival){int n=order.get(i).arrival;add(s,"IDLE",t,n);t=n;}while(i<order.size()&&order.get(i).arrival<=t)ready.add(order.get(i++));if(ready.isEmpty())continue;Proc p=ready.remove();f.putIfAbsent(p.id,t);int run=Math.min(q,rem.get(p.id));add(s,p.id,t,t+run);t+=run;rem.put(p.id,rem.get(p.id)-run);while(i<order.size()&&order.get(i).arrival<=t)ready.add(order.get(i++));if(rem.get(p.id)>0)ready.add(p);else d.put(p.id,t);}return finish(ps,s,f,d,"Round Robin (q="+q+")");}
    }
    private static class GanttPanel extends JPanel {
        private List<Seg> segments=new ArrayList<>(); GanttPanel(){setBackground(Color.WHITE);setPreferredSize(new Dimension(850,145));}
        void setSegments(List<Seg>s){segments=new ArrayList<>(s);int max=segments.stream().mapToInt(x->x.end).max().orElse(10);setPreferredSize(new Dimension(Math.max(850,max*48+70),145));revalidate();repaint();}
        protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D x=(Graphics2D)g.create();x.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);if(segments.isEmpty()){x.drawString("Run a simulation to display the Gantt chart.",20,30);x.dispose();return;}int left=30,top=25,h=42,scale=Math.max(25,Math.min(55,(getWidth()-60)/Math.max(1,segments.stream().mapToInt(s->s.end).max().orElse(1))));for(Seg s:segments){int px=left+s.start*scale,w=Math.max(1,(s.end-s.start)*scale);x.setColor(s.id.equals("IDLE")?new Color(225,225,225):color(s.id));x.fillRoundRect(px,top,w,h,8,8);x.setColor(Color.DARK_GRAY);x.drawRoundRect(px,top,w,h,8,8);if(w>30)x.drawString(s.id,px+w/2-x.getFontMetrics().stringWidth(s.id)/2,top+25);x.drawLine(px,top+h,px,top+h+8);x.drawString(""+s.start,px-3,top+h+22);}Seg last=segments.get(segments.size()-1);int end=left+last.end*scale;x.drawLine(end,top+h,end,top+h+8);x.drawString(""+last.end,end-3,top+h+22);x.drawString("Time",left,top+h+43);x.dispose();}
        private Color color(String id){Color[]c={new Color(104,149,209),new Color(112,173,71),new Color(237,125,49),new Color(128,100,162),new Color(75,172,198),new Color(192,80,77),new Color(155,187,89)};return c[Math.floorMod(id.hashCode(),c.length)];}
    }
    public static void main(String[] args){SwingUtilities.invokeLater(()->{try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}new CPUSchedulingSimulator().setVisible(true);});}
}
