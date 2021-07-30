package huancun
import chipsalliance.rocketchip.config.Parameters
import chisel3._
import chisel3.util._
import freechips.rocketchip.tilelink._
import TLMessages.{AccessAckData, ReleaseAck}

class SinkD(edge: TLEdgeOut)(implicit p: Parameters) extends HuanCunModule {
  val io = IO(new Bundle() {
    val d = Flipped(DecoupledIO(new TLBundleD(edge.bundle)))
    val bs_waddr = DecoupledIO(new DSAddress)
    val bs_wdata = Output(new DSData)
    val way = Input(UInt(wayBits.W))
    val set = Input(UInt(setBits.W))
    val resp = ValidIO(new SinkDResp)
    val sourceD_r_hazard = Flipped(ValidIO(new SourceDHazard))
  })

  val (first, last, _, beat) = edge.count(io.d)
  val uncache = io.d.bits.opcode === AccessAckData
  val cache = !uncache
  val needData = cache && io.d.bits.opcode =/= ReleaseAck
  val w_safe = !(io.sourceD_r_hazard.valid && io.sourceD_r_hazard.bits.safe(io.set, io.way))

  io.d.ready := cache && io.bs_waddr.ready && (!first || w_safe) // TODO: handle uncache access

  // Generate resp
  io.resp.valid := (first || last) && io.d.fire() // MSHR need to be notified when both first & last
  io.resp.bits.last := last
  io.resp.bits.opcode := io.d.bits.opcode
  io.resp.bits.param := io.d.bits.param
  io.resp.bits.source := io.d.bits.source
  io.resp.bits.sink := io.d.bits.sink
  io.resp.bits.denied := io.d.bits.denied

  // Save data to Datastorage
  io.bs_waddr.valid := (needData && io.d.valid && w_safe) || !first
  io.bs_waddr.bits.way := io.way
  io.bs_waddr.bits.set := io.set
  io.bs_waddr.bits.beat := Mux(io.d.valid, beat, RegEnable(beat + io.bs_waddr.ready.asUInt(), io.d.valid))
  io.bs_waddr.bits.write := true.B
  io.bs_waddr.bits.noop := !io.d.valid
  io.bs_wdata.data := io.d.bits.data
}
