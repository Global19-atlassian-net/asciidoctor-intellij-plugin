package org.asciidoc.intellij.psi;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.ResolveResult;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AsciiDocAttributeDeclarationReference extends PsiReferenceBase<PsiElement> implements PsiPolyVariantReference {
  private String key;

  public AsciiDocAttributeDeclarationReference(@NotNull PsiElement element, TextRange textRange) {
    super(element, textRange);
    key = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    Project project = myElement.getProject();
    final List<AsciiDocAttributeDeclaration> declarations = AsciiDocUtil.findAttributes(project, key, AsciiDocUtil.findAntoraModuleDir(myElement) != null);
    List<ResolveResult> results = new ArrayList<>();
    for (AsciiDocAttributeDeclaration declaration : declarations) {
      results.add(new PsiElementResolveResult(declaration.getNavigationElement()));
    }
    return results.toArray(new ResolveResult[0]);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    Project project = myElement.getProject();
    List<AttributeDeclaration> declarations = AsciiDocUtil.findAttributes(project, myElement);
    List<LookupElement> variants = new ArrayList<>();
    for (final AttributeDeclaration declaration : declarations) {
      if (declaration.getAttributeValue() != null) {
        String value = declaration.getAttributeValue();
        if (value == null) {
          value = "";
        } else {
          value = " (" + value + ")";
        }
        String attributeName = declaration.getAttributeName();
        LookupElementBuilder lb = LookupElementBuilder.create(attributeName)
          .withIcon(AsciiDocIcons.ASCIIDOC_ICON)
          .withCaseSensitivity(false)
          .withTailText(value, true)
          .withInsertHandler(getLookupElementInsertHandler(attributeName));
        if (declaration instanceof AsciiDocAttributeDeclaration) {
          lb = lb.withTypeText(((AsciiDocAttributeDeclaration) declaration).getContainingFile().getName());
        }
        variants.add(lb);
      }
    }
    return variants.toArray();
  }

  @NotNull
  private InsertHandler<LookupElement> getLookupElementInsertHandler(String attributeName) {
    return (insertionContext, item) -> {
      int offset = insertionContext.getStartOffset();
      PsiElement element = insertionContext.getFile().findElementAt(offset);
      if (element != null && element.getNode() != null) {
        if (element.getNode().getElementType() != AsciiDocTokenTypes.ATTRIBUTE_REF) {
          // the finalizing } hasn't been entered yet, autocomplete it here
          offset += attributeName.length();
          insertionContext.getDocument().insertString(offset, "}");
          offset += 1;
          insertionContext.getEditor().getCaretModel().moveToOffset(offset);
        } else {
          offset += attributeName.length();
          offset += 1; // skip the trailing curly brace
          insertionContext.getEditor().getCaretModel().moveToOffset(offset);
        }
      }
    };
  }
}
