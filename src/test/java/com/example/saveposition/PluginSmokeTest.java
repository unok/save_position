package com.example.saveposition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginSmokeTest {

    private ServerMock server;
    private SavePositionPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(SavePositionPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void saveCommandReturnsConfirmationMessage() {
        PlayerMock player = server.addPlayer();
        player.performCommand("pos save home");

        String first = player.nextMessage();
        assertNotNull(first, "保存メッセージが届くはず");
        assertTrue(first.contains("home"), "メッセージに保存名が含まれる: " + first);
    }

    @Test
    void listCommandShowsSavedPosition() {
        PlayerMock player = server.addPlayer();
        player.performCommand("pos save base");
        while (player.nextMessage() != null) { /* drain save messages */ }

        player.performCommand("pos list");
        String header = player.nextMessage();
        assertNotNull(header);
        assertTrue(header.contains("一覧"), "ヘッダ表示: " + header);

        boolean foundEntry = false;
        String line;
        while ((line = player.nextMessage()) != null) {
            if (line.contains("base")) {
                foundEntry = true;
                break;
            }
        }
        assertTrue(foundEntry, "list 結果に base が含まれる");
    }

    @Test
    void deleteCommandRemovesPosition() {
        PlayerMock player = server.addPlayer();
        player.performCommand("pos save tmp");
        while (player.nextMessage() != null) { /* drain */ }

        player.performCommand("pos delete tmp");
        String msg = player.nextMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("削除"), "削除メッセージ: " + msg);

        player.performCommand("pos show tmp");
        String after = player.nextMessage();
        assertNotNull(after);
        assertTrue(after.contains("見つかりません"), "見つからない応答: " + after);
    }

    @Test
    void shareThenSharedListIncludesEntry() {
        PlayerMock owner = server.addPlayer("Owner1");
        owner.performCommand("pos save spot");
        owner.performCommand("pos share spot");
        while (owner.nextMessage() != null) { /* drain */ }

        PlayerMock viewer = server.addPlayer("Viewer1");
        viewer.performCommand("pos shared");

        boolean found = false;
        String line;
        while ((line = viewer.nextMessage()) != null) {
            if (line.contains("spot") && line.contains("Owner1")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "shared 一覧に Owner1 の spot が含まれる");
    }

    @Test
    void gotoStartReturnsConfirmation() {
        PlayerMock player = server.addPlayer();
        player.performCommand("pos save home");
        while (player.nextMessage() != null) { /* drain save messages */ }

        player.performCommand("pos goto home");
        boolean found = false;
        String line;
        while ((line = player.nextMessage()) != null) {
            if (line.contains("home") && line.contains("ガイド")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "goto 開始メッセージに『home』とガイドが含まれる");
    }

    @Test
    void gotoUnknownReturnsNotFound() {
        PlayerMock player = server.addPlayer();
        player.performCommand("pos goto nope");

        String msg = player.nextMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("見つかりません"), "未保存名は見つかりません応答: " + msg);
    }

    @Test
    void gotoStopReturnsStoppedMessage() {
        PlayerMock player = server.addPlayer();
        player.performCommand("pos save home");
        player.performCommand("pos goto home");
        while (player.nextMessage() != null) { /* drain */ }

        player.performCommand("pos goto stop");
        String msg = player.nextMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("停止"), "停止メッセージ: " + msg);
    }

    @Test
    void gotoFallsBackToSharedPosition() {
        PlayerMock owner = server.addPlayer("Owner2");
        owner.performCommand("pos save base2");
        owner.performCommand("pos share base2");
        while (owner.nextMessage() != null) { /* drain */ }

        PlayerMock viewer = server.addPlayer("Viewer2");
        viewer.performCommand("pos goto base2");

        boolean found = false;
        String line;
        while ((line = viewer.nextMessage()) != null) {
            if (line.contains("base2") && line.contains("ガイド")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "共有地点 base2 でガイド開始: " + found);
    }
}
