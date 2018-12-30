object Test {
  def assertT(t: { true })  = ()
  def assertF(f: { false }) = ()

  dependent def r1 = 1 == 1    ; assertT(r1)
  dependent def r2 = "1" =="1" ; assertT(r2)
  dependent def r3 = 1 == 2    ; assertF(r3)
  dependent def r4 = "1" =="2" ; assertF(r4)
  dependent def r6 = 1 != 1    ; assertF(r6)
  dependent def r7 = "1" !="1" ; assertF(r7)
  dependent def r8 = 1 != 2    ; assertT(r8)
  dependent def r9 = "1" !="2" ; assertT(r9)
}
