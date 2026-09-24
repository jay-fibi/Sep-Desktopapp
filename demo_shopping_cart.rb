# Demo: simple shopping cart
class ShoppingCart
  def initialize
    @items = {}
  end

  def add_item(name, price, quantity = 1)
    @items[name] = { price: price, quantity: quantity }
  end

  def total
    @items.values.sum { |item| item[:price] * item[:quantity] }
  end

  def summary
    @items.each { |name, item| puts "#{name}: #{item[:quantity]} x $#{item[:price]}" }
    puts "Total: $#{'%.2f' % total}"
  end
end

cart = ShoppingCart.new
cart.add_item("Apple", 0.5, 4)
cart.add_item("Bread", 2.25)
cart.summary
