package emil

import cats.Applicative
import cats.implicits._

sealed trait MailBody[F[_]] {

  def withText(text: F[String]): MailBody[F]

  def withHtml(html: F[String]): MailBody[F]

  def fold[A](
      empty: MailBody.Empty[F] => A,
      text: MailBody.Text[F] => A,
      html: MailBody.Html[F] => A,
      both: MailBody.HtmlAndText[F] => A
  ): A

  /** Return the html or the text content. If only text is available, it
    * is applied to the given function that may convert it into html.
    */
  def htmlContent(txtToHtml: String => String)(implicit ev: Applicative[F]): F[String] =
    fold(_ => "".pure[F], txt => txt.text.map(txtToHtml), html => html.html, both => both.html)

  /** Return only the text part if present.
    */
  def textPart(implicit ev: Applicative[F]): F[Option[String]] =
    fold(
      _ => (None: Option[String]).pure[F],
      txt => txt.text.map(_.some),
      _ => (None: Option[String]).pure[F],
      both => both.text.map(_.some)
    )

  /** Return only the html part if present.
    */
  def htmlPart(implicit ev: Applicative[F]): F[Option[String]] =
    fold(
      _ => (None: Option[String]).pure[F],
      _ => (None: Option[String]).pure[F],
      html => html.html.map(_.some),
      both => both.html.map(_.some)
    )

  def isEmpty: Boolean =
    fold(_ => true, _ => false, _ => false, _ => false)

  def nonEmpty: Boolean =
    !isEmpty
}

object MailBody {

  final case class Empty[F[_]]() extends MailBody[F] {
    def withText(text: F[String]): MailBody[F] = Text(text)

    def withHtml(html: F[String]): MailBody[F] = Html(html)

    def fold[A](
        empty: MailBody.Empty[F] => A,
        text: MailBody.Text[F] => A,
        html: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = empty(this)

  }

  final case class Text[F[_]](text: F[String]) extends MailBody[F] {
    def withText(other: F[String]): MailBody[F] =
      Text(other)

    def withHtml(html: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def fold[A](
        empty: MailBody.Empty[F] => A,
        text: MailBody.Text[F] => A,
        html: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = text(this)
  }

  final case class Html[F[_]](html: F[String]) extends MailBody[F] {
    def withText(text: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def withHtml(other: F[String]): MailBody[F] =
      Html(other)

    def fold[A](
        empty: MailBody.Empty[F] => A,
        txt: MailBody.Text[F] => A,
        h: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = h(this)
  }

  final case class HtmlAndText[F[_]](text: F[String], html: F[String]) extends MailBody[F] {
    def withText(text: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def withHtml(html: F[String]): MailBody[F] =
      HtmlAndText(text, html)

    def fold[A](
        empty: MailBody.Empty[F] => A,
        txt: MailBody.Text[F] => A,
        h: MailBody.Html[F] => A,
        both: MailBody.HtmlAndText[F] => A
    ): A = both(this)
  }

  def empty[F[_]]: MailBody[F] =
    Empty()

  def text[F[_]: Applicative](text: String): MailBody[F] =
    if (text.isEmpty) empty[F] else Text(text.pure[F])

  def html[F[_]: Applicative](html: String): MailBody[F] =
    Html(html.pure[F])

  def both[F[_]: Applicative](text: String, html: String) =
    HtmlAndText(text.pure[F], html.pure[F])
}
