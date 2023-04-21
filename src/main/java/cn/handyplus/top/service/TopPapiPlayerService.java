package cn.handyplus.top.service;

import cn.handyplus.lib.core.CollUtil;
import cn.handyplus.lib.db.Compare;
import cn.handyplus.lib.db.Db;
import cn.handyplus.top.core.AsyncTask;
import cn.handyplus.top.enter.TopPapiPlayer;
import cn.handyplus.top.util.ConfigUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 玩家papi排行数据
 *
 * @author handy
 * @since 1.1.8
 */
public class TopPapiPlayerService {
    private TopPapiPlayerService() {
    }

    private static class SingletonHolder {
        private static final TopPapiPlayerService INSTANCE = new TopPapiPlayerService();
    }

    public static TopPapiPlayerService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 批量新增或更新数据
     *
     * @param topPapiPlayerList 批量记录
     * @since 1.2.2
     */
    public void replace(List<TopPapiPlayer> topPapiPlayerList) {
        if (CollUtil.isEmpty(topPapiPlayerList)) {
            return;
        }
        // 判断是否过滤op
        List<String> opUidList = new ArrayList<>();
        boolean isOp = ConfigUtil.CONFIG.getBoolean("isOp");
        if (isOp) {
            opUidList = AsyncTask.getOpUidList();
        }
        // 分组排序
        Map<String, List<TopPapiPlayer>> topPapiPlayerGroupList = topPapiPlayerList.stream().collect(Collectors.groupingBy(TopPapiPlayer::getPapi));
        for (String papi : topPapiPlayerGroupList.keySet()) {
            List<TopPapiPlayer> papiList = topPapiPlayerGroupList.get(papi);
            // 保存离线数据
            List<String> playerUuidList = topPapiPlayerList.stream().map(TopPapiPlayer::getPlayerUuid).distinct().collect(Collectors.toList());
            playerUuidList.addAll(opUidList);
            List<TopPapiPlayer> offTopPapiPlayerList = this.findByPlayerUuids(playerUuidList, papi);
            papiList.addAll(offTopPapiPlayerList);
            // 排序
            papiList = papiList.stream().sorted(Comparator.comparing(TopPapiPlayer::getVault).reversed()).collect(Collectors.toList());
            for (int i = 0; i < papiList.size(); i++) {
                papiList.get(i).setRank(i + 1);
            }
            topPapiPlayerGroupList.put(papi, papiList);
        }
        // 先删除
        this.delete();
        // ID赋值
        for (int i = 0; i < topPapiPlayerList.size(); i++) {
            topPapiPlayerList.get(i).setId(i + 1);
        }
        // 批量添加
        for (List<TopPapiPlayer> list : CollUtil.splitList(topPapiPlayerList, 1000)) {
            this.addBatch(list);
        }
    }

    /**
     * 根据名称分页查询
     *
     * @param papi     类型
     * @param pageNum  页数
     * @param pageSize 条数
     * @return TopPapiPlayer
     */
    public List<TopPapiPlayer> page(String papi, Integer pageNum, Integer pageSize) {
        Db<TopPapiPlayer> db = Db.use(TopPapiPlayer.class);
        Compare<TopPapiPlayer> where = db.where();
        where.limit(pageNum, pageSize).eq(TopPapiPlayer::getPapi, papi);
        where.orderByDesc(TopPapiPlayer::getVault);
        return db.execution().page().getRecords();
    }

    /**
     * 根据uid和类型查询
     *
     * @param uuid uid
     * @param type 类型
     * @return TopPapiPlayer
     * @since 1.2.2
     */
    public TopPapiPlayer findByUidAndType(String uuid, String type) {
        Db<TopPapiPlayer> db = Db.use(TopPapiPlayer.class);
        db.where().eq(TopPapiPlayer::getPlayerUuid, uuid)
                .eq(TopPapiPlayer::getPapi, type);
        return db.execution().selectOne();
    }

    /**
     * 根据排行和类型查询
     *
     * @param rank rank
     * @param type 类型
     * @return TopPapiPlayer
     * @since 1.2.2
     */
    public TopPapiPlayer findByRankAndType(Integer rank, String type) {
        Db<TopPapiPlayer> db = Db.use(TopPapiPlayer.class);
        db.where().eq(TopPapiPlayer::getRank, rank)
                .eq(TopPapiPlayer::getPapi, type);
        return db.execution().selectOne();
    }

    /**
     * 批量新增
     *
     * @param topPapiPlayerList 入参
     * @since 1.2.2
     */
    private void addBatch(List<TopPapiPlayer> topPapiPlayerList) {
        Db.use(TopPapiPlayer.class).execution().insertBatch(topPapiPlayerList);
    }

    /**
     * 删除
     *
     * @since 1.2.2
     */
    private void delete() {
        Db.use(TopPapiPlayer.class).execution().delete();
    }

    /**
     * 根据uid not in查询
     *
     * @param playerUuidList 用户uid
     * @param papi           变量
     * @return TopPapiPlayer
     * @since 1.2.5
     */
    public List<TopPapiPlayer> findByPlayerUuids(List<String> playerUuidList, String papi) {
        Db<TopPapiPlayer> db = Db.use(TopPapiPlayer.class);
        db.where().notIn(TopPapiPlayer::getPlayerUuid, playerUuidList)
                .eq(TopPapiPlayer::getPapi, papi);
        return db.execution().list();
    }

}