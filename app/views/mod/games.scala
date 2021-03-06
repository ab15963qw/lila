package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.game.Pov
import lila.user.User
import lila.evaluation.PlayerAssessment

object games {

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val sortNoneTh   = th(attr("data-sort-method") := "none")
  private val dataSort     = attr("data-sort")

  def apply(user: User, games: List[(Pov, Option[PlayerAssessment])])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} games",
      moreCss = cssTag("mod.games"),
      moreJs = jsModule("mod.games")
    ) {
      main(cls := "mod-games box")(
        postForm(action := routes.Analyse.multipleAnalysis(user.id), cls := "mod-games__form")(
          div(cls := "box__top")(
            h1(userLink(user), " games (WIP)"),
            div(cls := "box__top__actions")(
              submitButton(cls := "button")("Analyse")
            )
          ),
          table(cls := "mod-games game-list slist")(
            thead(
              tr(
                sortNoneTh(input(tpe := "checkbox", name := s"game[]", st.value := "all")),
                sortNumberTh("Opponent"),
                sortNumberTh("Perf"),
                th(iconTag('g')),
                sortNumberTh("Moves"),
                sortNumberTh("Result"),
                sortNumberTh("ACPL", br, "(Avg ± SD)"),
                sortNumberTh("Times", br, "(Avg ± SD)"),
                sortNumberTh("Blur"),
                sortNumberTh("Date")
              )
            ),
            tbody(
              games.map { case (pov, assessment) =>
                tr(
                  td(
                    input(
                      tpe := "checkbox",
                      name := s"game[]",
                      st.value := pov.gameId
                    )
                  ),
                  td(dataSort := ~pov.opponent.rating)(
                    playerLink(pov.opponent, withDiff = false)
                  ),
                  td(dataSort := pov.game.clock.??(_.config.estimateTotalSeconds))(
                    pov.game.perfType.map { pt =>
                      iconTag(pt.iconChar)(cls := "text")
                    },
                    shortClockName(pov.game.clock.map(_.config))
                  ),
                  td(dataSort := ~pov.game.tournamentId)(
                    pov.game.tournamentId map { tourId =>
                      a(
                        dataIcon := "g",
                        href := routes.Tournament.show(tourId).url,
                        title := tournamentIdToName(tourId)
                      )
                    }
                  ),
                  td(dataSort := pov.game.playedTurns)(pov.game.playedTurns),
                  td(dataSort := ~pov.player.ratingDiff)(
                    pov.win match {
                      case Some(true)  => goodTag(cls := "result")("1")
                      case Some(false) => badTag(cls := "result")("0")
                      case None        => span(cls := "result")("½")
                    },
                    pov.player.ratingDiff match {
                      case Some(d) if d > 0 => goodTag(s"+$d")
                      case Some(d) if d < 0 => badTag(d)
                      case _                => span("-")
                    }
                  ),
                  assessment match {
                    case Some(ass) =>
                      frag(
                        td(dataSort := ass.sfAvg)(s"${ass.sfAvg} ± ${ass.sfSd}"),
                        td(dataSort := ass.mtAvg)(
                          s"${ass.mtAvg / 10} ± ${ass.mtSd / 10}",
                          ~ass.mtStreak ?? frag(br, "STREAK")
                        ),
                        td(dataSort := ass.blurs)(
                          s"${ass.blurs}%",
                          ass.blurStreak.filter(8 <=) map { s =>
                            frag(br, s"STREAK $s/12")
                          }
                        )
                      )
                    case _ => td(colspan := 3)
                  },
                  td(dataSort := pov.game.movedAt.getMillis.toString)(
                    a(href := routes.Round.watcher(pov.gameId, pov.color.name))(
                      momentFromNowServer(pov.game.movedAt)
                    )
                  )
                )
              }
            )
          )
        )
      )
    }
}
