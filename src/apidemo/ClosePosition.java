

package apidemo;

import java.util.ArrayList;

import com.ib.controller.*;
import com.ib.controller.ApiConnection.ILogger;
import com.ib.controller.ApiController.IConnectionHandler;
import com.ib.controller.ApiController.IPositionHandler;
import com.ib.controller.ApiController.*;
import com.ib.controller.Types.Action;
import com.ib.controller.Types.MktDataType;
import com.ib.controller.Types.SecType;
import com.ib.controller.Types.TimeInForce;

/*Takes symbol on command line and creates a closing order at te bid/ask midpoint. */
public class ClosePosition  implements IConnectionHandler, ILogger {
	private final ApiController m_controller = new ApiController( this, this, this);
	private String m_symbol;
	private NewContract m_contract;
	private int m_position;
	private double m_bid;
	private double m_ask;
	private boolean m_placedOrder;
	
	public static void main(String[] args) {
		new ClosePosition().run("YHOO");
	}
	
	void run(String Symbol) {
		m_symbol = Symbol;
		m_controller.connect("127.0.0.1", 7496, 0);
	}
	
	@Override public void connected() {
		print("requesting position");
		
		m_controller.reqPositions(new IPositionHandler() {
			@Override public void position(String account, NewContract contract, int position, double avgCost) {
				if (contract.symbol().equals( m_symbol) && contract.secType() == SecType.STK) {
					m_contract = contract;
					m_position = position;
				}
			}
			
			@Override public void positionEnd() {
				onHavePosition();
			}
		});
	}
	
	@Override public void disconnected() {
	}

	@Override public void accountList(ArrayList<String> list) {
		
	}
	
	@Override public void error(Exception e) {
		
	}
	
	@Override public void message(int id, int errorCode, String errorMsg) {
		
	}
	
	@Override public void show(String string) {
		
	}
	
	protected void onHavePosition() {
		print("Current position is" + m_position);
		
		if (m_position != 0) {
			print("requesting market data");
			
			m_controller.reqTopMktData(m_contract, "", false,  new ITopMktDataHandler() {
				@Override public void tickPrice(NewTickType tickType, double price, int canAutoExecute) {
					switch( tickType) {
					case BID:
						m_bid = price;
						print("received bid" + price);
						break;
					case ASK:
						m_ask = price;
						print("received ask" + price);
						break;
					default:
						;
					}
					
					checkPrices( this);
				}
				
				@Override public void tickSize(NewTickType tickType, int size) {
					
				}
				
				@Override public void tickString(NewTickType tickType, String value) {
					
				}
				
				@Override public void tickSnapshotEnd() {
					
				}
				
				@Override public void marketDataType(MktDataType marketDataType) {
					
				}
			});
		}
		else {
			print( "There is no position to close");
			System.exit( 0);
		}
	}
	
	void checkPrices(ITopMktDataHandler handler) {
		if (m_bid != 0 && m_ask != 0 && !m_placedOrder) {
			m_placedOrder = true;
			
			print( "desubscribing market data");
			m_controller.cancelTopMktData(handler);
			
			placeOrder();
		}
	}
	
	void placeOrder() {
		double midPrice = Math.round( (m_bid + m_ask)/2*100)/100.0 + 1;
		
		m_contract.exchange( "SMART");
		m_contract.primaryExch( "ISLAND");
		
		NewOrder  order = new NewOrder();
		order.action(m_position > 0 ? Action.SELL : Action.BUY);
		order.totalQuantity( Math.abs(m_position));
		order.orderType( OrderType.LMT);
		order.lmtPrice( midPrice);
		order.tif( TimeInForce.DAY);
		
		print( "placing order " + order);
		
		m_controller.placeOrModifyOrder(m_contract, order, new IOrderHandler() {
			@Override public void handle(int errorCode, String errorMsg) {
				print( "Order code and message: " + errorCode +  " " + errorMsg);
			}
			

			@Override public void orderState(NewOrderState orderState) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void orderStatus(OrderStatus status, int filled, int remaining, double avgFillPrice, long permId,
					int parentId, double lastFillPrice, int clientId, String whyHeld) {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	@Override public void log(String valueof) {
		
	}
	
	void print(String str) {
		System.out.println(str);
	}
}



