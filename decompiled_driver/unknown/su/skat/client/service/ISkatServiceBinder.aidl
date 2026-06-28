package su.skat.client.service;

import su.skat.client.service.ISkatCommandCallback;
import su.skat.client.service.ISkatChatCallback;
import su.skat.client.service.ISkatStatusCallback;
import su.skat.client.service.ISkatLocationCallback;
import su.skat.client.service.ISkatRegionsCallback;
import su.skat.client.service.ISkatRegionCarsCallback;
import su.skat.client.service.ISkatOrderCallback;
import su.skat.client.service.ISkatOrdersCallback;
import su.skat.client.service.ISkatTaximeterCallback;
import su.skat.client.service.ISkatArticlesCallback;
import su.skat.client.service.ISkatPromisedPaymentsCallback;
import su.skat.client.service.ISkatFiscalCallback;
import su.skat.client.model.Order;
import su.skat.client.model.Article;
import su.skat.client.model.OrderExtra;
import su.skat.client.model.GlobalExtra;
import su.skat.client.model.Server;
import su.skat.client.model.ChatMessage;
import su.skat.client.model.ChatChannel;
import su.skat.client.model.Profile;
import su.skat.client.model.Region;
import su.skat.client.model.Rate;
import su.skat.client.model.PendingOrder;

interface ISkatServiceBinder {
    boolean isConnected();
    void setAuthInfo(String login, String pass);
    List<String> getAuthInfo();
    Server getCurrentServer();
	boolean checkServerVersion(String version);
	boolean hasServerFeature(String featureName);
    void setConnectionDemanded(boolean value);
    boolean getConnectionDemanded();
	int getConnectionState();
	void alert();
	void loadRegions();
	void loadRates();
	void loadRateRegions();
	void loadMessageTemplates();
	void loadOrderTimes();

	int[] getOrderTimeChoose();

	void registerCommandCallback(ISkatCommandCallback uiCallback);
	void unregisterCommandCallback(ISkatCommandCallback uiCallback);

	List<Region> getRegions();

	void registerStatusCallback(ISkatStatusCallback callback);
	void unregisterStatusCallback(ISkatStatusCallback callback);

	void registerRegionsCallback(ISkatRegionsCallback callback);
	void unregisterRegionsCallback(ISkatRegionsCallback callback);

	void registerRegionCarsCallback(ISkatRegionCarsCallback callback);
	void unregisterRegionCarsCallback(ISkatRegionCarsCallback callback);

	void registerLocationCallback(ISkatLocationCallback callback);
	void unregisterLocationCallback(ISkatLocationCallback callback);

	void fetchAttachedOrders();
	void registerOrdersCallback(ISkatOrdersCallback callback);
	void unregisterOrdersCallback(ISkatOrdersCallback callback);

	void registerOrderCallback(ISkatOrderCallback callback);
	void unregisterOrderCallback(ISkatOrderCallback callback);

	void registerTaximeterCallback(ISkatTaximeterCallback callback);
	void unregisterTaximeterCallback(ISkatTaximeterCallback callback);

	void registerArticlesCallback(ISkatArticlesCallback callback);
	void unregisterArticlesCallback(ISkatArticlesCallback callback);

	void registerPromisedPaymentsCallback(ISkatPromisedPaymentsCallback callback);
	void unregisterPromisedPaymentsCallback(ISkatPromisedPaymentsCallback callback);

	void fetchArticles();

	void registerOrderChatCallback(ISkatChatCallback chatCallback);
	void unregisterOrderChatCallback(ISkatChatCallback chatCallback);

	void registerFiscalCallback(ISkatFiscalCallback chatCallback);
	void unregisterFiscalCallback(ISkatFiscalCallback chatCallback);

	Order getOrder(int orderId);
	List<Order> getAssignedOrders();
	List<Order> getAssignedPreOrders();
	Location getLocation();
	Article getArticle(int id);

	void orderStartById(int orderId);
	void orderPauseById(int orderId);
	void orderStopById(int orderId);
	void orderRejectById(int orderId);
	int orderCloseById(int orderId);
	int orderCashAcceptedById(int orderId);

	void requestOrderStatusById(int orderId);

	void sendCommand(String msg);
	void sendEscapedCommand(String command, in String[] params);
	String sendRequest(String command, in String param);
	void setOrderTime(int time);
	void registry(int region);
	void autoRegistry(boolean force);

	void onplaceOrder();
	void orderOnPlaceById(int orderId);
	void orderCallClientById(int orderId);

	void startOrder();
	void clientNotOut();
	void orderReject();
	void orderOnTheRunReject();
	void orderComplite(int summ);
	void changePage(int pageNum);
	int getCurrentPage();
	void dontAskGPSSettings();
	void checkLogin();
	void getRegPosition();
	void getRgnState();
	String getChat();
	void sendSMS(String to, String msg);
	int getOrderTimer();
	void orderSyncRequest();
	void setBusyState(boolean context);
//	void getOrderStatus();
	Order getActiveOrder();
	int getActiveOrdersCount();
	int getAssignedOrdersCount();
	Order getLastCompletedOrder();
//	void cashAccepted();
	void bindOrder(int orderId, int time);
	void onTaxometerClosed();
	void setActiveOrder(in Order order);

	// resorders
	void setReservOrder(int reservOrderId);
	void resetReservOrder();
	void resetMyReservOrders();
	void cfrmReservOrder(int orderId);
	void rejectReservOrder(int orderId);
	Order reservOrderStart(int orderId);
	Order reservOrderOnPlace(int orderId);
	void resOrderMessageShowed();
	
	// taxometr
	boolean startTaxometr(int rateId);
	List<String> getRates();
	List<Rate> getVisibleRates();
	void pauseTaxometr();
	double stopTaxometr(double price, boolean noVoice);
	double stopTaxometrExtended(double price, boolean noVoice, boolean simulate);
	void standTaxometr();
	void changeRate(int rateId);
	String getBill(boolean printerFormating);
	String getBillDialog();
	String getOrderInfo();
	void requestOrderReceipt(int orderId, boolean cash_payment);

	void checkAdsCount();

	void clearArticles();
	void saveArticle(in Article article);
	void removeArticle(int id);
	List<Article> getArticlesList();
	Article getArticleByPushId(String pushId);
	void refreshReservOrders();
	List<Order> getReservOrders();

	List<OrderExtra> setExtras(in List<OrderExtra> extras);
	List<GlobalExtra> getGlobalExtras();

	List<ChatMessage> getChatChannelMessages(in ChatChannel channel);
	ChatChannel getChatChannel(String channelId);
    void chatSendMessage(in ChatChannel channel, in ChatMessage message, boolean resend);
    void chatChannelViewed(in ChatChannel channel);

    List<Profile> getAvailableProfilesByDistance();
    void setActiveProfile(int profileId);
    void forceProfileSync();
    void trySessionRequest();
    void clearSessionRequest();

	void requestRegionCars(int regionId);
	List<String> getGlobalChatTemplates();

	void stopService();
	void setAppInForeground(boolean inForeground);
	PendingOrder popPendingOrderIntent();
	boolean hasPendingOrderIntent();
	PendingOrder removePendingOrderIntent(int orderId);

	void removeNotification(int notificationId);
	void requestFreeOrder(int orderId);
	void requestResOrder(int orderId);

	// returns listener id
	long listenEvent(String stream, int event, in ResultReceiver receiver);
	void stopListenEvent(String stream, int event, long listenerId);

	void preventTaximeterNotification();
	String getEscPosTemplate(int orderId);
	void sendUserReport(String message);
}
