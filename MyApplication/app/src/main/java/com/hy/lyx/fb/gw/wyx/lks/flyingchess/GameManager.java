package com.hy.lyx.fb.gw.wyx.lks.flyingchess;

import android.os.Bundle;
import android.os.Message;

import java.util.Random;

/**
 * Created by karthur on 2016/4/9.
 */
public class GameManager {//game process control
    private GameWorker gw;//thread
    private ChessBoardAct board;
    public GameManager(){
        gw=new GameWorker();
    }

    public void newTurn(ChessBoardAct board){//call by activity when game start
        Game.getChessBoard().init();
        this.board=board;
        new Thread(gw).start();
    }

    public void gameOver(){
        gw.exit();
    }

    public void turnTo(int color){//call by other thread  be careful
        int dice, whichPlane;
        if(color == Game.getDataManager().getMyColor()){//it is my turn
            //get dice
            dice=Game.getPlayer().roll();
            //UI update
            for(int i = 0; i < 10; i++){
                Message msg = new Message();
                Bundle b = new Bundle();
                b.putString("dice",String.format("%d",Game.getChessBoard().getDice().roll()));
                msg.setData(b);
                msg.what=2;
                board.handler.sendMessage(msg);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.gc();
			Message msg = new Message();
            Bundle b = new Bundle();
            b.putString("dice",String.format("%d",dice));
            msg.setData(b);
            msg.what=2;
            board.handler.sendMessage(msg);
            if(Game.getPlayer().canIMove(color,dice)){//can move a plane
                //get plane
                do {
                    whichPlane=Game.getPlayer().choosePlane();
                }while(!Game.getPlayer().move(color,whichPlane,dice));
                ///UI update
                flyNow(color, whichPlane, dice);
            }
            else{

            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else{//others
            switch (Game.getDataManager().getGameMode()){
                case DataManager.GM_LOCAL: //local game
                {
                    Random r=new Random(System.currentTimeMillis());
                    dice=r.nextInt(6)+1;
                    //UI
                    for(int i=0;i<10;i++){
                        Message msg = new Message();
                        Bundle b = new Bundle();
                        b.putString("dice",String.format("%d",Game.getChessBoard().getDice().roll()));
                        msg.setData(b);
                        msg.what=2;
                        board.handler.sendMessage(msg);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    System.gc();
                    Message msg = new Message();
                    Bundle b = new Bundle();
                    b.putString("dice",String.format("%d",dice));
                    msg.setData(b);
                    msg.what=2;
                    board.handler.sendMessage(msg);
                    try {
                        Thread.sleep(750);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(Game.getPlayer().canIMove(color,dice)){
                        do{
                            whichPlane=r.nextInt(4);
                        }while(!Game.getPlayer().move(color,whichPlane,dice));
                        ///UI update

                        flyNow(color, whichPlane, dice);
                        //
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                    break;
                case DataManager.GM_LAN:
                    break;
                case DataManager.GM_WLAN:
                    break;
            }
        }

    }

    private void sendMessage(int color, int whichPlane, int pos, int what) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putInt("color", color);
        b.putInt("whichPlane", whichPlane);
        b.putInt("pos", pos);
        msg.setData(b);
        msg.what = what;
        board.handler.sendMessage(msg);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void flyNow(int color, int whichPlane, int dice) {
        int toPos = Game.getChessBoard().getAirplane(color).position[whichPlane];
        int curPos = Game.getChessBoard().getAirplane(color).curPos[whichPlane];
        if(curPos + dice == toPos || curPos == -1) {
            for (int pos = curPos + 1; pos <= toPos; pos++) {
                sendMessage(color, whichPlane, pos, 1);
            }
            crack(color, whichPlane, toPos);
        }
        else if(curPos + dice + 4 == toPos) { // short jump
            for(int pos = curPos + 1; pos <= curPos + dice; pos++) {
                sendMessage(color, whichPlane, pos, 1);
            }
            crack(color, whichPlane, curPos + dice);
            sendMessage(color, whichPlane, toPos, 1);
            crack(color, whichPlane, curPos);
        }
        else if(toPos == 30) { // short jump and then long jump
            for(int pos = curPos + 1; pos <= curPos + dice; pos++) {
                sendMessage(color, whichPlane, pos, 1);
            }
            crack(color, whichPlane, curPos + dice);
            sendMessage(color, whichPlane, 18, 1);
            crack(color, whichPlane, 18);
            sendMessage(color, whichPlane, 30, 1);
            crack(color, whichPlane, 30);
        }
        else if(toPos == 34) { // long jump and then short jump
            for(int pos =curPos + 1; pos <= 18; pos++) {
                sendMessage(color, whichPlane, pos, 1);
            }
            crack(color, whichPlane, 18);
            sendMessage(color, whichPlane, 30, 1);
            crack(color, whichPlane, 30);
            sendMessage(color, whichPlane, 34, 1);
            crack(color, whichPlane, 34);
        }
        else if(toPos == -2) {
            for (int pos = curPos + 1; pos <= 56; pos++) {
                sendMessage(color, whichPlane, pos, 1);
            }
        }
        else if(Game.getChessBoard().isOverflow()) { // overflow
            for (int pos = curPos + 1; pos <= 56; pos++) {
                sendMessage(color, whichPlane, pos, 1);
            }
            for(int pos = 55; pos >= toPos; pos--)
                sendMessage(color, whichPlane, pos, 1);
            crack(color, whichPlane, toPos);
            Game.getChessBoard().setOverflow(false);
        }
    }

    public void crack(int color, int whichPlane, int pos) {
        int crackColor = color;
        int crackPlane = whichPlane;
        int count = 0;
        for(int i = 0; i < 4; i++) {
            if(i != color) {
                for(int j = 0; j < 4; j++) {
                    int crackPos = Game.getChessBoard().getAirplane(i).position[j];
                    int factor = (i - color + 4) % 4;
                    if(pos != 0 && crackPos != 0 && crackPos == (pos + 13 * factor) % 52) {
                        crackPlane = j;
                        count++;
                    }
                }
                if(count == 1)
                    crackColor = i;
                if(count >= 1)
                    break;
            }
        }
        if(count >= 1) {
            sendMessage(crackColor, crackPlane, -1, 3);
            Game.getChessBoard().getAirplane(crackColor).position[crackPlane] = -1;
            Game.getChessBoard().getAirplane(crackColor).curPos[crackPlane] = -1;
        }
    }

}

class GameWorker implements Runnable{
    private boolean run;

    public GameWorker(){
        run=true;
    }

    @Override
    public void run() {
        int i=0;
        while(run){//control round
            i=(i%4);
            if(Game.getDataManager().getSiteState()[i]!=-1) {
                Game.getGameManager().turnTo(i);
            }
            i++;
        }
    }

    public void exit(){
        run=false;
    }
}